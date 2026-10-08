package com.wildlife.wildlife_conservationbackend.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolSummaryResponseDTO {
    private String id;
    private String assignmentId;
    private String routeId;
    private String parkId;
    private String rangerId;
    private String status;
    private Instant startedAt;
    private Instant endedAt;
    private double recordedDistanceMeters;
    private int waypointCount;
    private int observationCount;
    private Instant createdAt;
}
