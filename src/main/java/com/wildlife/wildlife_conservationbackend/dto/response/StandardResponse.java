package com.wildlife.wildlife_conservationbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandardResponse<T> {
    private String status;
    private String description;
    private T data;
    private ApiError error;
}
