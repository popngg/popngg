package gg.popn.http.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class PerformanceDiagnostics {
    private static final Map<String, String> QUERIES = queries();
    private final String prometheusUrl;
    private final ObjectMapper mapper;
    private final HttpClient client;

    @Autowired
    public PerformanceDiagnostics(
            @Value("${popngg.monitoring.prometheus-url:http://prometheus:9090}") String prometheusUrl,
            ObjectMapper mapper) {
        this(prometheusUrl, mapper, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300)).build());
    }

    PerformanceDiagnostics(String prometheusUrl, ObjectMapper mapper, HttpClient client) {
        this.prometheusUrl = prometheusUrl.replaceAll("/+$", "");
        this.mapper = mapper;
        this.client = client;
    }

    public Snapshot snapshot() {
        try {
            Map<String, CompletableFuture<Double>> pending = new LinkedHashMap<>();
            QUERIES.forEach((name, query) -> pending.put(name, query(query)));
            CompletableFuture.allOf(pending.values().toArray(CompletableFuture[]::new))
                    .get(1800, TimeUnit.MILLISECONDS);
            Map<String, Double> values = new LinkedHashMap<>();
            pending.forEach((name, future) -> values.put(name, future.join()));
            return new Snapshot(Instant.now(), values, null);
        } catch (Exception exception) {
            return new Snapshot(Instant.now(), Map.of(), "Prometheus 지표를 조회하지 못했습니다.");
        }
    }

    private CompletableFuture<Double> query(String expression) {
        String encoded = URLEncoder.encode(expression, StandardCharsets.UTF_8).replace("+", "%20");
        HttpRequest request = HttpRequest.newBuilder(URI.create(prometheusUrl + "/api/v1/query?query=" + encoded))
                .timeout(Duration.ofMillis(1200)).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
            try {
                JsonNode result = mapper.readTree(response.body()).path("data").path("result");
                if (!result.isArray() || result.isEmpty()) return null;
                String value = result.path(0).path("value").path(1).asText();
                double parsed = Double.parseDouble(value);
                return Double.isFinite(parsed) ? parsed : null;
            } catch (Exception ignored) {
                return null;
            }
        });
    }

    private static Map<String, String> queries() {
        Map<String, String> queries = new LinkedHashMap<>();
        String filter = "{job=\"popngg-api\",uri!~\"/actuator.*\"}";
        queries.put("requestRate", "sum(rate(http_server_requests_seconds_count" + filter + "[5m]))");
        queries.put("averageMs", "1000 * sum(increase(http_server_requests_seconds_sum" + filter
                + "[5m])) / sum(increase(http_server_requests_seconds_count" + filter + "[5m]))");
        queries.put("p95Ms", "1000 * histogram_quantile(0.95, sum by (le) (increase("
                + "http_server_requests_seconds_bucket" + filter + "[5m])))");
        queries.put("p99Ms", "1000 * histogram_quantile(0.99, sum by (le) (increase("
                + "http_server_requests_seconds_bucket" + filter + "[5m])))");
        queries.put("errorRate", "sum(rate(http_server_requests_seconds_count{job=\"popngg-api\","
                + "uri!~\"/actuator.*\",status=~\"5..\"}[5m])) / sum(rate("
                + "http_server_requests_seconds_count" + filter + "[5m]))");
        queries.put("apiCpu", "process_cpu_usage{job=\"popngg-api\"}");
        queries.put("systemCpu", "system_cpu_usage{job=\"popngg-api\"}");
        queries.put("hikariPending", "max(hikaricp_connections_pending{job=\"popngg-api\"})");
        queries.put("blockedThreads", "sum(jvm_threads_states_threads{job=\"popngg-api\",state=\"blocked\"})");
        return Map.copyOf(queries);
    }

    public record Snapshot(Instant collectedAt, Map<String, Double> values, String error) {
        public Double value(String name) { return values.get(name); }
        public boolean available() { return error == null; }
    }
}
