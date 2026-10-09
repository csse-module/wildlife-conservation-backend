package com.wildlife.wildlife_conservationbackend.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String errorCode;
    private String errorDescription;
    private Map<String, String> fieldErrors;
}
