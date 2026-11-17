package gg.popn.infra.notification;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JacketStorageTest {
    @Test
    void rejectsInvalidReplacementBeforeAnyStorageWrite() {
        var s3 = mock(S3Client.class);
        var storage = new S3JacketStorageAdapter(s3, "bucket", "static", "https://static.popn.gg");
        assertThatThrownBy(() -> storage.replacePng("hash", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(s3);
    }

    @Test
    void normalizesAtStorageBoundaryAndPublishesReplacementWithoutOverwritingOriginal() throws Exception {
        var s3 = mock(S3Client.class);
        var storage = new S3JacketStorageAdapter(s3, "bucket", "static", "https://static.popn.gg");
        var source = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(488, 116, BufferedImage.TYPE_INT_ARGB), "png", source);
        String url = storage.uploadVersionedPng("hash", source.toByteArray());
        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3).putObject(request.capture(), body.capture());
        byte[] png = body.getValue().contentStreamProvider().newStream().readAllBytes();
        var decoded = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(decoded.getWidth()).isEqualTo(244); assertThat(decoded.getHeight()).isEqualTo(58);
        assertThat(request.getValue().ifNoneMatch()).isEqualTo("*");
        assertThat(request.getValue().key()).isEqualTo("static/hash-jacket-" + S3JacketStorageAdapter.digest(png) + ".png");
        assertThat(url).isEqualTo("https://static.popn.gg/hash-jacket-" + S3JacketStorageAdapter.digest(png) + ".png");
        verify(s3, never()).copyObject(any(Consumer.class));
        verify(s3, never()).deleteObject(any(Consumer.class));
        assertThatThrownBy(() -> storage.readPng("https://evil.example/hash.png")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.readPng("https://static.popn.gg/../hash.png")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copiesTheCurrentVersionedObjectAndChecksDuplicateReplacementContent() throws Exception {
        var s3 = mock(S3Client.class);
        var storage = new S3JacketStorageAdapter(s3, "bucket", "static", "https://static.popn.gg");
        var bytes = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(244,58,BufferedImage.TYPE_INT_RGB),"png",bytes);
        byte[] png = bytes.toByteArray();
        when(s3.getObjectAsBytes(any(Consumer.class))).thenAnswer(invocation -> {
            var builder = GetObjectRequest.builder();
            ((Consumer<GetObjectRequest.Builder>) invocation.getArgument(0)).accept(builder);
            assertThat(builder.build().key()).startsWith("static/old-jacket-");
            return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), png);
        });
        assertThat(storage.copyFromUrl("https://static.popn.gg/old-jacket-digest.png", "new"))
                .isEqualTo("https://static.popn.gg/new.png");
        when(s3.putObject(any(PutObjectRequest.class),any(RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(412).build());
        assertThat(storage.uploadVersionedPng("old", png)).contains("old-jacket-");
        when(s3.getObjectAsBytes(any(Consumer.class))).thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(),new byte[]{1}));
        assertThatThrownBy(() -> storage.uploadVersionedPng("old", png)).isInstanceOf(IllegalStateException.class);
    }
}
