package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.enums.AlertStatus;
import com.wildlife.wildlife_conservationbackend.enums.RiskLevel;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseDTO {
    private String id;

    private String parkId;

    private String areaId;

    private String animal;

    private String collarId;

    private RiskLevel riskLevel;

    private Location location;

    private Instant locationUpdatedAt;

    private Instant detectedAt;

    private AlertStatus status;

    private Instant createdAt;

    private String assignedOfficerId;

    private Instant acceptedAt;

    private List<AlertSupportResponseDTO> supportRequests;

    private AlertResolutionResponseDTO response;
}
