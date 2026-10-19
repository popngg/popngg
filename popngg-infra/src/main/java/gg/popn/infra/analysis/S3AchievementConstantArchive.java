package gg.popn.infra.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AchievementConstantArchive;
import gg.popn.application.analysis.AchievementConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class S3AchievementConstantArchive implements AchievementConstantArchive {
    private final S3Client s3;private final ObjectMapper mapper;private final String bucket,prefix;
    public S3AchievementConstantArchive(S3Client s3,ObjectMapper mapper,
            @Value("${popngg.analysis.bucket:}")String bucket,
            @Value("${popngg.achievement-constants.prefix:achievement-constants}")String prefix){
        this.s3=s3;this.mapper=mapper;this.bucket=bucket;this.prefix=prefix.replaceAll("^/+|/+$","");
    }
    @Override public String archive(AchievementConstants.Import snapshot){
        if(bucket.isBlank())throw new IllegalStateException("ANALYSIS_BUCKET_NOT_CONFIGURED");
        var access=s3.getPublicAccessBlock(request->request.bucket(bucket)).publicAccessBlockConfiguration();
        if(!Boolean.TRUE.equals(access.blockPublicAcls())||!Boolean.TRUE.equals(access.ignorePublicAcls())
                ||!Boolean.TRUE.equals(access.blockPublicPolicy())||!Boolean.TRUE.equals(access.restrictPublicBuckets()))
            throw new IllegalStateException("ANALYSIS_BUCKET_MUST_BLOCK_PUBLIC_ACCESS");
        try{
            byte[] bytes=mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(snapshot);
            String id=sha256(snapshot.sourceSnapshotId()).substring(0,32);
            String content=sha256(bytes).substring(0,16);
            String key=prefix+"/snapshots/"+id+"/"+snapshot.axis().name().toLowerCase()+"/"+content+"/achievement-import.json";
            s3.putObject(request->request.bucket(bucket).key(key).contentType("application/json")
                    .serverSideEncryption(ServerSideEncryption.AES256),RequestBody.fromBytes(bytes));
            return "s3://"+bucket+"/"+key;
        }catch(JsonProcessingException exception){throw new IllegalArgumentException("Invalid achievement snapshot",exception);}
    }
    private static String sha256(String value){
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }
    private static String sha256(byte[] value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value));}
        catch(NoSuchAlgorithmException exception){throw new IllegalStateException(exception);}
    }
}
