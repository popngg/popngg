package gg.popn.infra.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gg.popn.application.analysis.AchievementConstants;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3AchievementConstantArchiveTest {
    @Test void archivesJsonOnlyInAPrivateEncryptedBucket(){
        var s3=mock(S3Client.class);
        when(s3.getPublicAccessBlock(any(Consumer.class))).thenReturn(GetPublicAccessBlockResponse.builder()
                .publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder().blockPublicAcls(true)
                        .ignorePublicAcls(true).blockPublicPolicy(true).restrictPublicBuckets(true).build()).build());
        var request=new AtomicReference<PutObjectRequest>();
        when(s3.putObject(any(Consumer.class),any(RequestBody.class))).thenAnswer(invocation->{
            var builder=PutObjectRequest.builder();
            ((Consumer<PutObjectRequest.Builder>)invocation.getArgument(0)).accept(builder);
            request.set(builder.build());return PutObjectResponse.builder().build();
        });
        var archive=new S3AchievementConstantArchive(s3,new ObjectMapper().registerModule(new JavaTimeModule()),"private","achievement-constants");
        var snapshot=new AchievementConstants.Import("source:1","achievement-v1","EXPERIMENTAL",
                Instant.parse("2026-09-25T00:00:00Z"),AchievementConstants.Axis.MEDAL,List.of());
        String uri=archive.archive(snapshot);
        assertThat(uri).startsWith("s3://private/achievement-constants/snapshots/")
                .contains("/medal/").endsWith("/achievement-import.json");
        assertThat(request.get().serverSideEncryption()).isEqualTo(ServerSideEncryption.AES256);
        assertThat(request.get().contentType()).isEqualTo("application/json");
    }
}
