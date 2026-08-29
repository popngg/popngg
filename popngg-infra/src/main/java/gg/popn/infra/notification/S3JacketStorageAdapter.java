package gg.popn.infra.notification;

import gg.popn.application.song.port.out.JacketStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3JacketStorageAdapter implements JacketStoragePort {
    private final S3Client s3;
    private final String bucket, prefix, publicUrl;

    public S3JacketStorageAdapter(S3Client s3,
            @Value("${popngg.jacket.bucket:}") String bucket,
            @Value("${popngg.jacket.prefix:static}") String prefix,
            @Value("${popngg.jacket.public-url:https://static.popn.gg}") String publicUrl) {
        this.s3 = s3;
        this.bucket = bucket;
        this.prefix = prefix.replaceAll("^/+|/+$", "");
        this.publicUrl = publicUrl.replaceAll("/+$", "");
    }

    @Override
    public String uploadPng(String songHash, byte[] png) {
        if (bucket.isBlank()) throw new IllegalStateException("AWS_S3_BUCKET is not configured.");
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key(songHash))
                        .contentType("image/png")
                        .cacheControl("public,max-age=31536000,immutable")
                        .ifNoneMatch("*").build(), RequestBody.fromBytes(png));
        return publicUrl + "/" + songHash + ".png";
    }

    @Override
    public void delete(String songHash) {
        s3.deleteObject(request -> request.bucket(bucket).key(key(songHash)));
    }

    @Override
    public String copy(String sourceSongHash, String targetSongHash) {
        if (bucket.isBlank()) throw new IllegalStateException("AWS_S3_BUCKET is not configured.");
        try {
            s3.headObject(request -> request.bucket(bucket).key(key(targetSongHash)));
            throw new IllegalStateException("Target jacket already exists: " + targetSongHash);
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException ignored) {
            // Expected for a new song hash.
        } catch (software.amazon.awssdk.services.s3.model.S3Exception exception) {
            if (exception.statusCode() != 404) throw exception;
        }
        s3.copyObject(request -> request.bucket(bucket).key(key(targetSongHash))
                .copySource(bucket + "/" + key(sourceSongHash))
                .metadataDirective("COPY"));
        return publicUrl + "/" + targetSongHash + ".png";
    }

    @Override
    public String replacePng(String songHash, byte[] png) {
        String backupKey = "backup/" + songHash + "/" + System.currentTimeMillis() + ".png";
        s3.copyObject(request -> request.bucket(bucket).key(backupKey)
                .copySource(bucket + "/" + key(songHash)).metadataDirective("COPY"));
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key(songHash))
                        .contentType("image/png").cacheControl("public,max-age=31536000,immutable").build(),
                RequestBody.fromBytes(png));
        return backupKey;
    }

    @Override
    public void restore(String songHash, String backupKey) {
        s3.copyObject(request -> request.bucket(bucket).key(key(songHash))
                .copySource(bucket + "/" + backupKey).metadataDirective("COPY"));
    }

    private String key(String songHash) {
        return (prefix.isBlank() ? "" : prefix + "/") + songHash + ".png";
    }
}
