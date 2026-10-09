package com.wildlife.wildlife_conservationbackend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraReviewRequest {
    private String species;

    private Boolean possiblePoacher;

    private String notes;
}
