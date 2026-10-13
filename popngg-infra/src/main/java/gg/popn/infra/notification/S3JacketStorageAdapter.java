package gg.popn.infra.notification;

import gg.popn.application.song.port.out.JacketStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import gg.popn.infra.storage.JacketImageProcessor;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Arrays;

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
                        .ifNoneMatch("*").build(), RequestBody.fromBytes(JacketImageProcessor.normalize(png)));
        return publicUrl + "/" + songHash + ".png";
    }

    @Override
    public String uploadVersionedPng(String songHash, byte[] source) {
        if (bucket.isBlank()) throw new IllegalStateException("AWS_S3_BUCKET is not configured.");
        byte[] png = JacketImageProcessor.normalize(source);
        String name = songHash + "-jacket-" + digest(png);
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key(name))
                    .contentType("image/png").cacheControl("public,max-age=31536000,immutable")
                    .ifNoneMatch("*").build(), RequestBody.fromBytes(png));
        } catch (software.amazon.awssdk.services.s3.model.S3Exception exception) {
            if (exception.statusCode() != 412) throw exception;
            byte[] existing = s3.getObjectAsBytes(request -> request.bucket(bucket).key(key(name))).asByteArray();
            if (!Arrays.equals(existing, png)) throw new IllegalStateException("Existing jacket content does not match its digest.", exception);
        }
        return publicUrl + "/" + name + ".png";
    }

    public byte[] readPng(String jacketUrl) {
        String beginning = publicUrl + "/";
        if (jacketUrl == null || !jacketUrl.startsWith(beginning))
            throw new IllegalArgumentException("자켓 URL이 설정된 저장소에 속하지 않습니다.");
        String name = jacketUrl.substring(beginning.length());
        if (!name.matches("[A-Za-z0-9_-]+\\.png"))
            throw new IllegalArgumentException("올바른 자켓 경로가 아닙니다.");
        return s3.getObjectAsBytes(request -> request.bucket(bucket)
                .key((prefix.isBlank() ? "" : prefix + "/") + name)).asByteArray();
    }

    @Override
    public String copyFromUrl(String sourceJacketUrl, String targetSongHash) {
        return uploadPng(targetSongHash, readPng(sourceJacketUrl));
    }

    public static String digest(byte[] png) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png)); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    @Override
    public void delete(String songHash) {
        s3.deleteObject(request -> request.bucket(bucket).key(key(songHash)));
    }

    @Override
    public String copy(String sourceSongHash, String targetSongHash) {
        return copyFromUrl(publicUrl + "/" + sourceSongHash + ".png", targetSongHash);
    }

    @Override
    public String replacePng(String songHash, byte[] png) {
        byte[] normalized = JacketImageProcessor.normalize(png);
        String backupKey = "backup/" + songHash + "/" + System.currentTimeMillis() + ".png";
        s3.copyObject(request -> request.bucket(bucket).key(backupKey)
                .copySource(bucket + "/" + key(songHash)).metadataDirective("COPY"));
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key(songHash))
                        .contentType("image/png").cacheControl("public,max-age=31536000,immutable").build(),
                RequestBody.fromBytes(normalized));
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
