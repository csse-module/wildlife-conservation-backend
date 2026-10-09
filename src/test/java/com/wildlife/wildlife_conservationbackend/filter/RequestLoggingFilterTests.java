package com.wildlife.wildlife_conservationbackend.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.wildlife.wildlife_conservationbackend.utility.LogSanitizer;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestLoggingFilterTests {
    private final LogSanitizer sanitizer = new LogSanitizer(new ObjectMapper());
    private final RequestLoggingFilter filter = new RequestLoggingFilter(sanitizer);
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>() {
        @Override
        protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    };
    private Level previousLevel;

    @BeforeEach
    void captureLogs() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
        MDC.clear();
    }

    @AfterEach
    void restoreLogging() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
        MDC.clear();
    }

    @Test
    void redactsCredentialsAndLocationsWithoutChangingHttpBodies() throws Exception {
        String requestBody = "{\"email\":\"private@example.com\",\"password\":\"private-password\","
                + "\"nested\":{\"API_KEY\":\"private-api-key\"},\"location\":{\"latitude\":6.12345},"
                + "\"trackPoints\":[{\"longitude\":81.54321}],\"parkId\":\"park-yala\"}";
        String responseBody = "{\"data\":{\"accessToken\":\"private-access-token\","
                + "\"user\":{\"id\":\"user-1\",\"name\":\"Private Person\",\"email\":\"private@example.com\"}}}";
        MockHttpServletRequest request = request(requestBody);
        request.addHeader("Authorization", "Bearer private-bearer-token");
        request.addHeader("X-API-Key", "private-header-key");
        request.addHeader("Cookie", "session=private-cookie");
        request.addHeader("X-Unknown-Secret", "private-unknown-secret");
        request.addHeader("X-Request-ID", "demo-request");
        request.addHeader("X-B3-TraceId", "5DE11E2ADB817950");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (incoming, outgoing) -> {
            assertThat(new String(incoming.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(requestBody);
            assertThat(MDC.get("requestId")).isEqualTo("demo-request");
            assertThat(MDC.get("traceId")).isEqualTo("5de11e2adb817950");
            outgoing.setContentType("application/json");
            outgoing.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
        });
        assertThat(response.getContentAsString()).isEqualTo(responseBody);
        assertThat(response.getHeader("X-Request-ID")).isEqualTo("demo-request");
        assertThat(response.getHeader("X-Trace-ID")).isEqualTo("5de11e2adb817950");
        String logs = messages();
        assertThat(logs).contains("REQUEST BEGIN", "REQUEST END", "RESPONSE BEGIN", "RESPONSE END",
                "Status code  : 200", "Status text  : OK", "[REDACTED]", "[OMITTED]", "park-yala", "user-1");
        assertThat(logs).doesNotContain("private-password", "private-access-token", "private-bearer-token",
                "private-api-key", "private-header-key", "private-cookie", "private-unknown-secret",
                "private@example.com", "Private Person", "6.12345", "81.54321");
        assertThat(appender.list).allSatisfy(event -> assertThat(event.getMDCPropertyMap())
                .containsEntry("traceId", "5de11e2adb817950").containsEntry("requestId", "demo-request"));
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void largePayloadsPassThroughWhileTheirLogCaptureStaysBounded() throws Exception {
        String body = "{\"password\":\"private-large-password\",\"description\":\"" + "x".repeat(40_000) + "\"}";
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(body), response, (incoming, outgoing) -> {
            assertThat(incoming.getInputStream().readAllBytes()).isEqualTo(body.getBytes(StandardCharsets.UTF_8));
            outgoing.setContentType("application/json");
            outgoing.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        });
        assertThat(response.getContentAsString()).isEqualTo(body);
        assertThat(messages()).contains("JSON body exceeds 16384 bytes").doesNotContain("private-large-password");
        assertThat(messages().length()).isLessThan(3000);
    }

    @Test
    void binaryDownloadsRemainIntactAndDoNotAppearInLogs() throws Exception {
        byte[] binary = new byte[80_000];
        Arrays.fill(binary, (byte) 42);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/media/image/content");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (incoming, outgoing) -> {
            outgoing.setContentType("image/png");
            outgoing.setContentLength(binary.length);
            outgoing.getOutputStream().write(binary);
        });
        assertThat(response.getContentAsByteArray()).isEqualTo(binary);
        assertThat(response.getContentLength()).isEqualTo(binary.length);
        assertThat(messages()).contains("binary, multipart or non-JSON content").doesNotContain("*".repeat(20));
    }

    @Test
    void sanitizesLogInjectionAndRestoresOuterContextAfterFailures() {
        MDC.put("outer", "outer-context");
        MDC.put("requestId", "previous-request");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/parks\r\nforged-log");
        request.addHeader("X-Request-ID", "malicious\nrequest");
        request.addHeader("X-B3-TraceId", "malicious\ntrace");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> filter.doFilter(request, response, (incoming, outgoing) -> {
            throw new ServletException("simulated failure");
        })).isInstanceOf(ServletException.class);
        assertThat(response.getHeader("X-Request-ID")).matches("[a-f0-9-]{36}");
        assertThat(response.getHeader("X-Trace-ID")).matches("[a-f0-9]{16}");
        assertThat(messages()).doesNotContain("\r", "\n").contains("status=500");
        assertThat(MDC.getCopyOfContextMap()).containsExactlyInAnyOrderEntriesOf(
                Map.of("outer", "outer-context", "requestId", "previous-request"));
    }

    @Test
    void infoLoggingAvoidsBodyWrappersButKeepsRequestSummary() throws Exception {
        logger.setLevel(Level.INFO);
        MockHttpServletRequest request = request("{\"password\":\"private-password\"}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (incoming, outgoing) -> {
            assertThat(incoming).isSameAs(request);
            assertThat(outgoing).isSameAs(response);
            outgoing.setContentType("application/json");
            outgoing.getOutputStream().write("{}".getBytes(StandardCharsets.UTF_8));
        });
        assertThat(messages()).contains("REQUEST BEGIN", "RESPONSE END", "status=200").doesNotContain("body", "private-password");
        assertThat(appender.list).allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.INFO));
    }

    @Test
    void writerResponsesAndBufferResetKeepTheirOriginalServletSemantics() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("{}"), response, (incoming, outgoing) -> {
            outgoing.setCharacterEncoding("UTF-8");
            outgoing.setContentType("application/json");
            outgoing.getOutputStream().write("{\"old\":true}".getBytes(StandardCharsets.UTF_8));
            outgoing.reset();
            outgoing.setCharacterEncoding("UTF-8");
            outgoing.setContentType("application/json");
            outgoing.getWriter().write("{\"message\":\"Elephant 🐘\"}");
        });
        assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Elephant 🐘\"}");
        assertThat(messages()).contains("Elephant").doesNotContain("\"old\":true");
    }

    @Test
    void errorResponsesDiscardEarlierCapturedData() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("{}"), response, (incoming, outgoing) -> {
            outgoing.getOutputStream().write("old-response".getBytes(StandardCharsets.UTF_8));
            ((jakarta.servlet.http.HttpServletResponse) outgoing).sendError(404, "Not found");
        });
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(messages()).contains("Status code  : 404", "Status text  : Not Found").doesNotContain("old-response");
    }

    @Test
    void invalidAndNonJsonBodiesAreSummarizedWithoutRawText() {
        assertThat(sanitizer.body("private-text".getBytes(StandardCharsets.UTF_8), "text/plain", false))
                .contains("non-JSON").doesNotContain("private-text");
        assertThat(sanitizer.body("{\"password\":\"private-text\"".getBytes(StandardCharsets.UTF_8), "application/json", false))
                .contains("invalid JSON").doesNotContain("private-text");
        assertThat(sanitizer.headers(Map.of("User-Agent", List.of("safe\r\nforged"), "Authorization", List.of("Bearer secret"))))
                .containsEntry("User-Agent", "safe__forged").containsEntry("Authorization", "[REDACTED]");
    }

    private MockHttpServletRequest request(String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return request;
    }

    private String messages() {
        return String.join(" | ", appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }
}
