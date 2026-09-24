package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import gg.popn.application.analysis.RatingQuery;
import gg.popn.application.analysis.RatingSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import java.time.*;
import java.util.Map;

@Component
public class S3RatingQuery implements RatingQuery {
    private final S3Client s3; private final ObjectMapper mapper; private final String bucket,prefix;
    private volatile Cached cached;
    public S3RatingQuery(S3Client s3,ObjectMapper mapper,
            @Value("${popngg.analysis.bucket:}") String bucket,
            @Value("${popngg.analysis.prefix:cpi-spi}") String prefix) {
        this.s3=s3;this.mapper=mapper;this.bucket=bucket;this.prefix=prefix.replaceAll("^/+|/+$","");
    }
    @Override public RatingSnapshot latest() {
        var current=cached;
        if(current!=null && current.expiresAt().isAfter(Instant.now()))return current.snapshot();
        synchronized(this) {
            current=cached;if(current!=null && current.expiresAt().isAfter(Instant.now()))return current.snapshot();
            if(bucket.isBlank())throw new IllegalStateException("RATINGS_NOT_CONFIGURED");
            try {
                byte[] latest=s3.getObjectAsBytes(r->r.bucket(bucket).key(prefix+"/latest.json")).asByteArray();
                @SuppressWarnings("unchecked") var pointer=mapper.readValue(latest,Map.class);
                String snapshotId=String.valueOf(pointer.get("snapshotId"));
                byte[] bytes=s3.getObjectAsBytes(r->r.bucket(bucket).key(prefix+"/snapshots/"+snapshotId+"/ratings.json")).asByteArray();
                var raw=mapper.readValue(bytes,RatingSnapshot.class);
                var snapshot=new RatingSnapshot(snapshotId,raw.generatedAt(),raw.modelVersion(),raw.modelStatus(),raw.minimumPlayers(),raw.charts());
                cached=new Cached(snapshot,Instant.now().plusSeconds(60));return snapshot;
            } catch(Exception e) {throw new IllegalStateException("RATINGS_NOT_READY",e);}
        }
    }
    private record Cached(RatingSnapshot snapshot,Instant expiresAt){}
}
