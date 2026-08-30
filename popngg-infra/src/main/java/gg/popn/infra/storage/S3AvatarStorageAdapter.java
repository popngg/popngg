package gg.popn.infra.storage;

import gg.popn.application.account.port.out.AvatarStoragePort;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@Component
public class S3AvatarStorageAdapter implements AvatarStoragePort {
    private final S3Client s3;
    private final String bucket;
    private final String prefix;
    private final String publicUrl;

    public S3AvatarStorageAdapter(S3Client s3,
            @Value("${popngg.avatar.bucket:}") String bucket,
            @Value("${popngg.avatar.prefix:avatars}") String prefix,
            @Value("${popngg.avatar.public-url:https://static.popn.gg}") String publicUrl) {
        this.s3 = s3;
        this.bucket = bucket;
        this.prefix = prefix.replaceAll("^/+|/+$", "");
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @Override
    public String upload(String poptomoId, byte[] bytes, String contentType) {
        if (bucket.isBlank()) throw new IllegalStateException("AWS_S3_BUCKET is not configured.");
        String extension = switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> throw new IllegalArgumentException("Unsupported avatar content type.");
        };
        String key = prefix + "/" + poptomoId + "/" + UUID.randomUUID() + "." + extension;
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType)
                        .cacheControl("public,max-age=31536000,immutable").build(),
                RequestBody.fromBytes(bytes));
        return publicUrl + "/" + key;
    }

    @Override
    public void deleteIfManaged(String url) {
        String expected = publicUrl + "/" + prefix + "/";
        if (url == null || !url.startsWith(expected)) return;
        String key = URI.create(url).getPath().replaceFirst("^/", "");
        if (!key.startsWith(prefix + "/")) return;
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
