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
    private final String storagePrefix;
    private final String publicPrefix;
    private final String publicUrl;

    public S3AvatarStorageAdapter(S3Client s3,
            @Value("${popngg.avatar.bucket:}") String bucket,
            @Value("${popngg.avatar.storage-root:static}") String storageRoot,
            @Value("${popngg.avatar.prefix:avatars}") String publicPrefix,
            @Value("${popngg.avatar.public-url:https://static.popn.gg}") String publicUrl) {
        this.s3 = s3;
        this.bucket = bucket;
        String normalizedRoot = storageRoot.replaceAll("^/+|/+$", "");
        this.publicPrefix = publicPrefix.replaceAll("^/+|/+$", "");
        this.storagePrefix = normalizedRoot.isBlank()
                ? this.publicPrefix
                : normalizedRoot + "/" + this.publicPrefix;
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
        String objectPath = poptomoId + "/" + UUID.randomUUID() + "." + extension;
        String relativePath = publicPrefix + "/" + objectPath;
        String key = storagePrefix + "/" + objectPath;
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType)
                        .cacheControl("public,max-age=31536000,immutable").build(),
                RequestBody.fromBytes(bytes));
        return publicUrl + "/" + relativePath;
    }

    @Override
    public void deleteIfManaged(String url) {
        String expected = publicUrl + "/" + publicPrefix + "/";
        if (url == null || !url.startsWith(expected)) return;
        String relativePath = URI.create(url).getPath().replaceFirst("^/", "");
        if (!relativePath.startsWith(publicPrefix + "/")) return;
        String key = storagePrefix + relativePath.substring(publicPrefix.length());
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
