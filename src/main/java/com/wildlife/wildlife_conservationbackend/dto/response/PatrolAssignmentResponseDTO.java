package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.enums.AssignmentStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolAssignmentResponseDTO {
    private String id;
    private String parkId;
    private String routeId;
    private String rangerId;
    private Instant scheduledStartAt;
    private Instant scheduledEndAt;
    private AssignmentStatus status;
    private String assignedBy;
    private Instant createdAt;
}
