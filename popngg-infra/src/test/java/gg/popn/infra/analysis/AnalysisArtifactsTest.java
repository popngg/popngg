package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalysisArtifactsTest {
    @TempDir Path dir;
    @Test void uploadsManifestLastAndOnlyExplicitlyPublishesLatest()throws Exception {
        var s3=mock(S3Client.class);var requests=new ArrayList<PutObjectRequest>();
        when(s3.putObject(any(Consumer.class),any(RequestBody.class))).thenAnswer(inv->{
            PutObjectRequest.Builder b=PutObjectRequest.builder();
            ((Consumer<PutObjectRequest.Builder>)inv.getArgument(0)).accept(b);requests.add(b.build());return PutObjectResponse.builder().build();});
        var adapter=new AnalysisArtifacts(s3,new ObjectMapper(),"private-bucket","cpi-spi","test");
        Files.writeString(dir.resolve("report.md"),"report");
        String uri=adapter.upload("job",dir,Map.of(),Map.of());
        assertThat(uri).isEqualTo("s3://private-bucket/cpi-spi/snapshots/job/manifest.json");
        assertThat(requests).extracting(PutObjectRequest::key).containsExactly("cpi-spi/snapshots/job/report.md","cpi-spi/snapshots/job/manifest.json");
        assertThat(requests).allMatch(r->r.serverSideEncryption()==ServerSideEncryption.AES256);
        adapter.publishLatest("job",uri);
        assertThat(requests.getLast().key()).isEqualTo("cpi-spi/latest.json");
        assertThat(Files.readString(dir.resolve("manifest.json"))).contains("sha256","linear interpolation");
    }
    @Test void refusesPublicOrUnconfiguredBucket() {
        var s3=mock(S3Client.class);
        when(s3.getPublicAccessBlock(any(Consumer.class))).thenReturn(GetPublicAccessBlockResponse.builder()
                .publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder().blockPublicAcls(true).build()).build());
        assertThatThrownBy(()->new AnalysisArtifacts(s3,new ObjectMapper(),"public","cpi","test").checkPrivateBucket())
                .hasMessage("ANALYSIS_BUCKET_MUST_BLOCK_PUBLIC_ACCESS");
        assertThatThrownBy(()->new AnalysisArtifacts(s3,new ObjectMapper(),"","cpi","test").checkPrivateBucket())
                .hasMessage("ANALYSIS_BUCKET_NOT_CONFIGURED");
    }
}
