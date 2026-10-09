package com.wildlife.wildlife_conservationbackend.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraImageRequest {
    private String cameraTrapId;

    private String mediaId;

    private Instant capturedAt;
}
