package com.wildlife.wildlife_conservationbackend.config;

import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ResponseGenerator responseGenerator;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        if (exception instanceof OAuth2AuthenticationException oauth
                && "temporarily_unavailable".equals(oauth.getError().getErrorCode())) {
            log.error("Authentication storage unavailable path={}", request.getRequestURI());
            write(response, HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE", "The service is temporarily unavailable.");
            return;
        }
        response.setHeader("WWW-Authenticate", "Bearer");
        log.warn("Authentication rejected path={}", request.getRequestURI());
        write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid access token is required.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        log.warn("Access denied path={}", request.getRequestURI());
        write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Your role cannot perform this operation.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        var generated = responseGenerator.generateErrorResponse(status, code, message);
        response.setStatus(generated.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), generated.getBody());
    }
}
