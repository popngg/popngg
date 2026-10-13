package gg.popn.infra.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.infra.notification.S3JacketStorageAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JacketMaintenanceCliTest {
    @TempDir Path directory;
    final ObjectMapper mapper = new ObjectMapper();
    final Connection connection = mock(Connection.class);
    final S3Client s3 = mock(S3Client.class);

    void catalog(String[] hashes, String[] urls) throws Exception {
        var query = mock(PreparedStatement.class); var rows = mock(ResultSet.class);
        when(connection.prepareStatement(startsWith("SELECT"))).thenReturn(query);
        when(query.executeQuery()).thenReturn(rows);
        var index = new java.util.concurrent.atomic.AtomicInteger(-1);
        when(rows.next()).thenAnswer(i -> index.incrementAndGet() < hashes.length);
        when(rows.getLong(1)).thenAnswer(i -> (long) index.get()+1);
        when(rows.getString(2)).thenAnswer(i -> hashes[index.get()]);
        when(rows.getString(3)).thenAnswer(i -> urls[index.get()]);
        when(rows.getInt(4)).thenReturn(29);
    }

    @Test void dryRunFitsExistingImagesPreservesExclusionsAndMakesNoExternalWrites() throws Exception {
        byte[] portrait = png(100,200), banner = png(244,58);
        Files.write(directory.resolve("1.png"), png(488,116));
        Files.writeString(directory.resolve("catalog-before.json"), "[{\"songId\":1,\"songHash\":\"hash1\"}]");
        Files.writeString(directory.resolve("excluded-song-ids.txt"), "3\n");
        catalog(new String[]{"hash1","hash2","hash3","hash4","hash5"},new String[]{"https://static.popn.gg/1.png","https://static.popn.gg/2.png","https://static.popn.gg/3.png","https://static.popn.gg/4.png",null});
        when(s3.getObjectAsBytes(any(Consumer.class))).thenAnswer(i -> {
            var b=GetObjectRequest.builder(); ((Consumer<GetObjectRequest.Builder>)i.getArgument(0)).accept(b);
            return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(),b.build().key().equals("static/2.png")?portrait:banner);
        });
        Path report=directory.resolve("report.json");
        run(report,"--normalize-existing");
        var rows=mapper.readTree(report.toFile());
        assertThat(rows.size()).isEqualTo(2);
        assertThat(rows.get(0).path("source").asText()).isEqualTo("official");
        assertThat(rows.get(1).path("source").asText()).isEqualTo("existing");
        assertThat(rows.get(0).path("status").asText()).isEqualTo("planned");
        assertThat(rows.get(1).path("newUrl").asText()).contains(S3JacketStorageAdapter.digest(JacketImageProcessor.normalize(portrait)));
        verify(s3,never()).putObject(any(PutObjectRequest.class),any(RequestBody.class));
        verify(connection,never()).prepareStatement(startsWith("UPDATE"));
    }

    @Test void applyUsesImmutableObjectAndGuardsCurrentHashAndUrlAndSkipsOnRetry() throws Exception {
        byte[] banner=png(244,58); Files.write(directory.resolve("1.png"),banner);
        Files.writeString(directory.resolve("catalog-before.json"),"[{\"songId\":1,\"songHash\":\"hash\"}]");
        String old="https://static.popn.gg/hash.png";
        catalog(new String[]{"hash"},new String[]{old});
        var update=mock(PreparedStatement.class);
        when(connection.prepareStatement(startsWith("UPDATE"))).thenAnswer(i -> {
            assertThat((String)i.getArgument(0)).contains("BINARY song_hash=BINARY ?","BINARY jacket_url=BINARY ?");return update;
        });
        when(update.executeUpdate()).thenReturn(1);
        Path report=directory.resolve("report.json");run(report,"--apply");
        String url=mapper.readTree(report.toFile()).get(0).path("newUrl").asText();
        verify(update).setString(1,url);verify(update).setLong(2,1L);verify(update).setString(3,"hash");verify(update).setString(4,old);
        verify(s3).putObject(argThat((PutObjectRequest r)->r.ifNoneMatch().equals("*")),any(RequestBody.class));
        assertThat(mapper.readTree(report.toFile()).get(0).path("status").asText()).isEqualTo("applied");
        clearInvocations(s3,update);catalog(new String[]{"hash"},new String[]{url});run(report,"--apply");
        assertThat(mapper.readTree(report.toFile()).size()).isZero();verifyNoInteractions(s3,update);
    }

    @Test void rejectsChangedIdentityAndStopsAtConcurrentUrlChange() throws Exception {
        Files.write(directory.resolve("1.png"),png(244,58));
        Files.writeString(directory.resolve("catalog-before.json"),"[{\"songId\":1,\"songHash\":\"hash\"}]");
        catalog(new String[]{"changed-hash"},new String[]{"https://static.popn.gg/old.png"});
        assertThatThrownBy(()->run(directory.resolve("report.json"),"--apply")).hasMessageContaining("identity");verifyNoInteractions(s3);
        catalog(new String[]{"hash"},new String[]{"https://static.popn.gg/old.png"});
        var update=mock(PreparedStatement.class);when(connection.prepareStatement(startsWith("UPDATE"))).thenReturn(update);
        when(update.executeUpdate()).thenReturn(0);
        assertThatThrownBy(()->run(directory.resolve("report.json"),"--apply")).hasMessageContaining("concurrently");
        verify(s3,never()).deleteObject(any(Consumer.class));
    }

    @Test void offlineNormalizerUsesTheSameProcessor() throws Exception {
        Path source=Files.createDirectory(directory.resolve("source")),target=directory.resolve("output");
        byte[] image=png(100,200);Files.write(source.resolve("1.png"),image);Files.writeString(source.resolve("ignore.txt"),"ignored");
        JacketNormalizeCli.main(new String[]{source.toString(),target.toString()});
        assertThat(Files.readAllBytes(target.resolve("1.png"))).isEqualTo(JacketImageProcessor.normalize(image));
        assertThat(Files.exists(target.resolve("ignore.txt"))).isFalse();
        assertThatThrownBy(()->JacketNormalizeCli.main(new String[]{})).isInstanceOf(IllegalArgumentException.class);
    }

    void run(Path report,String... flags) throws Exception {
        var args=new java.util.ArrayList<String>();args.add(directory.toString());args.add(report.toString());args.addAll(java.util.List.of(flags));
        JacketMaintenanceCli.run(args.toArray(String[]::new),connection,s3,"bucket","static","https://static.popn.gg");
    }
    byte[] png(int w,int h) throws Exception {
        var b=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB),"png",b);return b.toByteArray();
    }
}
