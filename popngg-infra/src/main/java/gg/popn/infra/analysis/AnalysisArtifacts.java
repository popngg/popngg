package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.AnalysisStatistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

@Component
public class AnalysisArtifacts {
    private final S3Client s3; private final ObjectMapper mapper; private final String bucket,prefix,build;
    public AnalysisArtifacts(S3Client s3,ObjectMapper mapper,
            @Value("${popngg.analysis.bucket:}") String bucket,
            @Value("${popngg.analysis.prefix:cpi-spi}") String prefix,
            @Value("${POPNGG_GIT_SHA:unknown}") String build) {
        this.s3=s3;this.mapper=mapper;this.bucket=bucket;this.prefix=prefix.replaceAll("^/+|/+$","");this.build=build;
    }
    public void checkPrivateBucket() {
        if(bucket.isBlank()) throw new IllegalStateException("ANALYSIS_BUCKET_NOT_CONFIGURED");
        var b=s3.getPublicAccessBlock(r->r.bucket(bucket)).publicAccessBlockConfiguration();
        if(!Boolean.TRUE.equals(b.blockPublicAcls()) || !Boolean.TRUE.equals(b.ignorePublicAcls())
                || !Boolean.TRUE.equals(b.blockPublicPolicy()) || !Boolean.TRUE.equals(b.restrictPublicBuckets()))
            throw new IllegalStateException("ANALYSIS_BUCKET_MUST_BLOCK_PUBLIC_ACCESS");
    }
    public String upload(String jobId,Path directory,Map<String,Object> source,Map<String,Object> summary) throws IOException {
        var files=new ArrayList<Map<String,Object>>();
        try(var paths=Files.list(directory)) {
            for(Path p:paths.filter(Files::isRegularFile).sorted().toList()) {
                if(p.getFileName().toString().equals("manifest.json")) continue;
                String key=prefix+"/snapshots/"+jobId+"/"+p.getFileName();
                files.add(Map.of("name",p.getFileName().toString(),"bytes",Files.size(p),"sha256",sha256(p),"s3Uri",uri(key)));
                putFile(key,p);
            }
        }
        var manifest=new LinkedHashMap<String,Object>();
        manifest.put("jobId",jobId.split("/",2)[0]);manifest.put("snapshotId",jobId);
        manifest.put("policy",AnalysisStatistics.POLICY);manifest.put("build",build);
        manifest.put("source",source);manifest.put("summary",summary);manifest.put("files",files);
        manifest.put("quantile","linear interpolation at (n-1)*q");
        manifest.put("readinessSplit","Long.hashCode(chartId * 0x9E3779B97F4A7C15L) & 1; even reference, odd validation");
        Path path=directory.resolve("manifest.json");mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(),manifest);
        String key=prefix+"/snapshots/"+jobId+"/manifest.json";putFile(key,path);return uri(key);
    }
    /** Called only after all artifacts and manifest have uploaded successfully. */
    public void publishLatest(String jobId,String manifest) throws IOException {
        byte[] bytes=mapper.writeValueAsBytes(Map.of("jobId",jobId.split("/",2)[0],"snapshotId",jobId,"manifest",manifest,"policy",AnalysisStatistics.POLICY));
        s3.putObject(r->r.bucket(bucket).key(prefix+"/latest.json").contentType("application/json")
                .serverSideEncryption(ServerSideEncryption.AES256),RequestBody.fromBytes(bytes));
    }
    private void putFile(String key,Path file) {
        String name=file.getFileName().toString();
        String type=name.endsWith(".json")?"application/json":name.endsWith(".jsonl")?"application/x-ndjson":name.endsWith(".csv")?"text/csv; charset=utf-8":"text/markdown; charset=utf-8";
        s3.putObject(r->r.bucket(bucket).key(key).contentType(type).serverSideEncryption(ServerSideEncryption.AES256),RequestBody.fromFile(file));
    }
    private String uri(String key) {return "s3://"+bucket+"/"+key;}
    static String sha256(byte[] bytes) {return HexFormat.of().formatHex(digest().digest(bytes));}
    private static String sha256(Path p)throws IOException {
        var digest=digest();try(var stream=new DigestInputStream(Files.newInputStream(p),digest)){stream.transferTo(OutputStream.nullOutputStream());}
        return HexFormat.of().formatHex(digest.digest());
    }
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
