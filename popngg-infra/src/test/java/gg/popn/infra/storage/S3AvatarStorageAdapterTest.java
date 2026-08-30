package gg.popn.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3AvatarStorageAdapterTest {
    private final S3Client s3 = mock(S3Client.class);
    private final S3AvatarStorageAdapter storage = new S3AvatarStorageAdapter(
            s3, "bucket", "/static/", "/avatars/", "https://static.popn.gg/");

    @Test
    void uploadsVersionedImmutableObjectAndReturnsPublicUrl() {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(null);

        String url = storage.upload("1234-5678-9012", new byte[]{1}, "image/webp");

        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("bucket");
        assertThat(request.getValue().key()).startsWith("static/avatars/1234-5678-9012/").endsWith(".webp");
        assertThat(request.getValue().contentType()).isEqualTo("image/webp");
        assertThat(request.getValue().cacheControl()).isEqualTo("public,max-age=31536000,immutable");
        assertThat(url).isEqualTo("https://static.popn.gg/"
                + request.getValue().key().substring("static/".length()));
    }

    @Test
    void onlyDeletesUrlsManagedByAvatarPrefix() {
        storage.deleteIfManaged("https://elsewhere.example/avatar.png");
        verify(s3, never()).deleteObject(any(DeleteObjectRequest.class));

        storage.deleteIfManaged("https://static.popn.gg/avatars/1234/file.png");
        var request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(request.capture());
        assertThat(request.getValue().key()).isEqualTo("static/avatars/1234/file.png");
    }
}
