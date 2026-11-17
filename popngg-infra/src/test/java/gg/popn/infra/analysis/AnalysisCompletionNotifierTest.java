package gg.popn.infra.analysis;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;

class AnalysisCompletionNotifierTest {
    @Test void sendsActualJsonAttachmentAndDoesNotSwallowHttpFailure()throws Exception {
        var body=new AtomicReference<String>();var status=new AtomicInteger(204);
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",x->{body.set(new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));x.sendResponseHeaders(status.get(),-1);x.close();});
        server.start();
        try {
            var notifier=new AnalysisCompletionNotifier("http://127.0.0.1:"+server.getAddress().getPort(),HttpClient.newHttpClient());
            notifier.send("{\"jobId\":\"123\",\"status\":\"SUCCEEDED\"}");
            assertThat(body.get()).contains("filename=\"analysis-result.json\"","\"status\":\"SUCCEEDED\"","admin bot","allowed_mentions");
            notifier.send("{\"jobType\":\"ACHIEVEMENT_CONSTANTS\",\"status\":\"SUCCEEDED\"}");
            assertThat(body.get()).contains("filename=\"achievement-result.json\"", "상수 최신화 작업 결과입니다.");
            status.set(429);
            assertThatThrownBy(()->notifier.send("{}")).isInstanceOf(IllegalStateException.class).hasMessage("ADMIN_NOTIFICATION_HTTP_429");
            assertThatThrownBy(()->new AnalysisCompletionNotifier("").send("{}")).hasMessage("ADMIN_WEBHOOK_NOT_CONFIGURED");
        }finally{server.stop(0);}
    }
}
