package gg.popn.http.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class RequestObservabilityFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_ATTRIBUTE = RequestObservabilityFilter.class.getName() + ".traceId";
    private static final Logger log = LoggerFactory.getLogger(RequestObservabilityFilter.class);
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private final long slowRequestMillis;
    private final String release;

    public RequestObservabilityFilter(
            @Value("${popngg.monitoring.slow-request-millis:1000}") long slowRequestMillis,
            @Value("${POPNGG_RELEASE_VERSION:local}") String release) {
        this.slowRequestMillis = slowRequestMillis;
        this.release = release;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = traceId(request);
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        long started = System.nanoTime();
        MDC.put("traceId", traceId);
        MDC.put("method", request.getMethod());
        MDC.put("release", release);
        response.setHeader("X-Request-ID", traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMillis = (System.nanoTime() - started) / 1_000_000;
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String route = pattern == null ? "unmatched" : pattern.toString();
            MDC.put("uri", route);
            MDC.put("status", Integer.toString(response.getStatus()));
            MDC.put("durationMs", Long.toString(durationMillis));
            if (durationMillis >= slowRequestMillis) {
                log.warn("Slow HTTP request completed");
            } else if (response.getStatus() >= 500) {
                log.error("HTTP request completed with server error");
            }
            MDC.clear();
        }
    }

    private static String traceId(HttpServletRequest request) {
        String candidate = request.getHeader("X-Request-ID");
        return candidate != null && SAFE_TRACE_ID.matcher(candidate).matches()
                ? candidate : UUID.randomUUID().toString();
    }
}
