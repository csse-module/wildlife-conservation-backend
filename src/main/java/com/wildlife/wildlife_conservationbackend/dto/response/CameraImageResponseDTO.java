package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.enums.CameraImageStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraImageResponseDTO {
    private String id;

    private String cameraTrapId;

    private String mediaId;

    private Instant capturedAt;

    private String parkId;

    private CameraImageStatus status;

    private String uploadedBy;

    private Instant createdAt;

    private CameraReviewResponseDTO review;
}
