package com.wildlife.wildlife_conservationbackend.domain;

import com.wildlife.wildlife_conservationbackend.enums.RiskLevel;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSetupRequest {
    private String parkId;

    private String areaId;

    private String animal;

    private String collarId;

    private RiskLevel riskLevel;

    private Location location;

    private Instant locationUpdatedAt;

    private Instant detectedAt;
}
