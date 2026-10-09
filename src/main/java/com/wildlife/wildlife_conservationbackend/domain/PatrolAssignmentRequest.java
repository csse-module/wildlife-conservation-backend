package com.wildlife.wildlife_conservationbackend.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolAssignmentRequest {
    private String routeId;
    private String rangerId;
    private Instant scheduledStartAt;
    private Instant scheduledEndAt;
}
