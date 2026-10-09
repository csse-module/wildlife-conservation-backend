package com.wildlife.wildlife_conservationbackend.filter;

import com.wildlife.wildlife_conservationbackend.utility.LogSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    private final LogSanitizer sanitizer;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        String requestId = requestId(request.getHeader("X-Request-ID"));
        String traceId = traceId(request);
        boolean detailed = log.isDebugEnabled();
        BoundedRequestWrapper capturedRequest = detailed ? new BoundedRequestWrapper(request, LogSanitizer.MAX_BODY_BYTES) : null;
        BoundedResponseWrapper capturedResponse = detailed ? new BoundedResponseWrapper(response, LogSanitizer.MAX_BODY_BYTES) : null;
        long started = System.nanoTime();
        boolean completed = false;
        MDC.put("requestId", requestId);
        MDC.put("traceId", traceId);
        response.setHeader("X-Request-ID", requestId);
        response.setHeader("X-Trace-ID", traceId);
        try {
            logRequest(request, detailed);
            chain.doFilter(detailed ? capturedRequest : request, detailed ? capturedResponse : response);
            completed = true;
        } finally {
            try {
                if (capturedResponse != null && completed) {
                    capturedResponse.finishCapture();
                }
                logResponse(request, response, capturedRequest, capturedResponse, started, completed);
            } finally {
                if (previousContext == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previousContext);
                }
            }
        }
    }

    private void logRequest(HttpServletRequest request, boolean detailed) {
        log.info("=========================== REQUEST BEGIN ===========================");
        if (detailed) {
            log.debug("URI          : {}", sanitizer.clean(request.getRequestURI(), 2048));
            log.debug("Method       : {}", sanitizer.clean(request.getMethod(), 20));
            Map<String, List<String>> headers = new LinkedHashMap<>();
            Collections.list(request.getHeaderNames()).forEach(name -> headers.put(name, Collections.list(request.getHeaders(name))));
            log.debug("Headers      : {}", sanitizer.headers(headers));
        }
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response,
                             BoundedRequestWrapper capturedRequest, BoundedResponseWrapper capturedResponse,
                             long started, boolean completed) {
        if (capturedRequest != null) {
            log.debug("Request body : {}", sanitizer.body(capturedRequest.getContentAsByteArray(),
                    request.getContentType(), capturedRequest.isTruncated()));
        }
        log.info("=========================== REQUEST END =============================");
        log.info("=========================== RESPONSE BEGIN ==========================");
        int status = completed || response.getStatus() >= 400 ? response.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR.value();
        if (capturedResponse != null) {
            HttpStatus httpStatus = HttpStatus.resolve(status);
            log.debug("Status code  : {}", status);
            log.debug("Status text  : {}", httpStatus == null ? "Unknown" : httpStatus.getReasonPhrase());
            Map<String, Collection<String>> headers = new LinkedHashMap<>();
            response.getHeaderNames().forEach(name -> headers.put(name, response.getHeaders(name)));
            log.debug("Headers      : {}", sanitizer.headers(headers));
            log.debug("Response body: {}", sanitizer.body(capturedResponse.getContentAsByteArray(),
                    response.getContentType(), capturedResponse.isTruncated()));
        }
        log.info("HTTP method={} path={} status={} durationMs={}", sanitizer.clean(request.getMethod(), 20),
                sanitizer.clean(request.getRequestURI(), 2048), status, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        log.info("=========================== RESPONSE END ============================");
    }

    private String requestId(String incoming) {
        return incoming != null && incoming.matches("[A-Za-z0-9-]{1,64}") ? incoming : UUID.randomUUID().toString();
    }

    private String traceId(HttpServletRequest request) {
        String incoming = request.getHeader("X-B3-TraceId");
        if (incoming == null) {
            incoming = request.getHeader("X-Trace-ID");
        }
        return incoming != null && incoming.matches("(?i)([a-f0-9]{16}|[a-f0-9]{32})") && !incoming.matches("0+")
                ? incoming.toLowerCase(Locale.ROOT) : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
