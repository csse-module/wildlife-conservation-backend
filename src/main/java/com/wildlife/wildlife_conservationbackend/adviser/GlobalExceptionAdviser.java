package com.wildlife.wildlife_conservationbackend.adviser;

import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionAdviser {
    private final ResponseGenerator responseGenerator;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> business(ApiException exception) {
        log.warn("Request rejected code={} status={}", exception.getCode(), exception.getStatus().value());
        return responseGenerator.generateErrorResponse(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> validation(BindException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
        return responseGenerator.generateErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Check the highlighted fields.", fields);
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<StandardResponse<Map<String, Object>>> parameters(Exception exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (exception instanceof HandlerMethodValidationException validation) {
            validation.getParameterValidationResults().forEach(result -> {
                if (result instanceof ParameterErrors errors) {
                    errors.getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(),
                            error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
                } else {
                    String name = result.getMethodParameter().getParameterName();
                    result.getResolvableErrors().forEach(error -> fields.putIfAbsent(name == null ? "parameter" : name,
                            error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()));
                }
            });
        } else if (exception instanceof ConstraintViolationException validation) {
            validation.getConstraintViolations().forEach(error -> fields.putIfAbsent(error.getPropertyPath().toString(), error.getMessage()));
        }
        return responseGenerator.generateErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Check the highlighted fields.", fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<StandardResponse<Map<String, Object>>> malformed(Exception exception) {
        return responseGenerator.generateErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The request contains missing or invalid values.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> forbidden(AccessDeniedException exception) {
        return responseGenerator.generateErrorResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Your role cannot perform this operation.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> notFound(NoResourceFoundException exception) {
        return responseGenerator.generateErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "The endpoint was not found.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> methodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return responseGenerator.generateErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "This HTTP method is not supported.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> unsupportedMedia(HttpMediaTypeNotSupportedException exception) {
        return responseGenerator.generateErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Use the content type required by this endpoint.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> fileTooLarge(MaxUploadSizeExceededException exception) {
        return responseGenerator.generateErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Images must not exceed 5 MiB.");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> multipart(MultipartException exception) {
        return responseGenerator.generateErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The multipart upload is invalid.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> storage(DataAccessException exception) {
        log.error("Database operation failed", exception);
        return responseGenerator.generateErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE", "The service is temporarily unavailable.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardResponse<Map<String, Object>>> unexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        return responseGenerator.generateErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }
}
