package com.wildlife.wildlife_conservationbackend.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraReviewResponseDTO {
    private String species;

    private Boolean possiblePoacher;

    private String notes;

    private String reviewedBy;

    private Instant reviewedAt;
}
