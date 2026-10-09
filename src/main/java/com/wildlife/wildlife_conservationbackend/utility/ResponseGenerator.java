package com.wildlife.wildlife_conservationbackend.utility;

import com.wildlife.wildlife_conservationbackend.domain.SaveResult;
import com.wildlife.wildlife_conservationbackend.dto.response.ApiError;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ResponseGenerator {
    private static final String SUCCESS_CODE = "00";
    private static final String FAILURE_CODE = "01";
    private static final String SUCCESS_DESCRIPTION = "SUCCESS";
    private static final String FAILURE_DESCRIPTION = "FAIL";

    public <T> ResponseEntity<StandardResponse<T>> generateSuccessResponse(T data, HttpStatus status) {
        return ResponseEntity.status(status).body(successBody(data));
    }

    public <T> ResponseEntity<StandardResponse<T>> generateSuccessResponse(SaveResult<T> result, URI resourceUri) {
        HttpStatus status = result.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (result.isCreated()) {
            response.location(resourceUri);
        }
        return response.body(successBody(result.getData()));
    }

    public ResponseEntity<StandardResponse<Map<String, Object>>> generateErrorResponse(
            HttpStatus status, String code, String message) {
        return generateErrorResponse(status, code, message, Map.of());
    }

    public ResponseEntity<StandardResponse<Map<String, Object>>> generateErrorResponse(
            HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
        ApiError error = new ApiError(code, message, Map.copyOf(fieldErrors));
        StandardResponse<Map<String, Object>> body = new StandardResponse<>(
                FAILURE_CODE, FAILURE_DESCRIPTION, Map.of(), error);
        return ResponseEntity.status(status).body(body);
    }

    private <T> StandardResponse<T> successBody(T data) {
        return new StandardResponse<>(SUCCESS_CODE, SUCCESS_DESCRIPTION, data,
                new ApiError(SUCCESS_CODE, SUCCESS_DESCRIPTION, Map.of()));
    }
}
