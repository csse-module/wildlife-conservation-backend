package com.wildlife.wildlife_conservationbackend.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackPoint {
    private Double latitude;

    private Double longitude;

    private Instant recordedAt;

    private Double accuracyMeters;
}
