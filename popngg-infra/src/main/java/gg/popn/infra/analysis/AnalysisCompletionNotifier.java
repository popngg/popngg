package gg.popn.infra.analysis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;

/** Separate admin notification: never depends on the expiring Discord interaction token. */
@Component
public class AnalysisCompletionNotifier {
    private final String webhook; private final HttpClient client;
    @org.springframework.beans.factory.annotation.Autowired
    public AnalysisCompletionNotifier(@Value("${popngg.discord.admin-webhook-url:}") String webhook) {
        this(webhook,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    }
    AnalysisCompletionNotifier(String webhook,HttpClient client) {this.webhook=webhook;this.client=client;}
    public void send(String json) throws Exception {
        if(webhook.isBlank()) throw new IllegalStateException("ADMIN_WEBHOOK_NOT_CONFIGURED");
        String boundary="analysis-"+UUID.randomUUID();
        boolean achievement = "ACHIEVEMENT_CONSTANTS".equals(new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(json).path("jobType").asText());
        String text = achievement ? "상수 최신화 작업 결과입니다. 첨부 JSON의 status를 확인하고, 성공 시 /상수표를 사용해 주세요."
                : "CPI/SPI 분석 작업 결과입니다. 첨부 JSON의 status를 확인해 주세요.";
        String payload="{\"username\":\"admin bot\",\"content\":\""+text+"\",\"allowed_mentions\":{\"parse\":[]}}";
        String filename = achievement ? "achievement-result.json" : "analysis-result.json";
        String body="--"+boundary+"\r\nContent-Disposition: form-data; name=\"payload_json\"\r\nContent-Type: application/json\r\n\r\n"+payload
                +"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"files[0]\"; filename=\""+filename+"\"\r\nContent-Type: application/json\r\n\r\n"+json
                +"\r\n--"+boundary+"--\r\n";
        HttpRequest request=HttpRequest.newBuilder(URI.create(webhook)).timeout(Duration.ofSeconds(15))
                .header("Content-Type","multipart/form-data; boundary="+boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        int status=client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode();
        if(status<200 || status>=300) throw new IllegalStateException("ADMIN_NOTIFICATION_HTTP_"+status);
    }
}
