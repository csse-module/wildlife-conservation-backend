package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.enums.IncidentType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponseDTO {
    private String id;

    private String parkId;

    private String areaId;

    private String assignmentId;

    private IncidentType type;

    private Instant detectedAt;

    private Location location;

    private String description;

    private String photoId;

    private String reportedBy;

    private String status;

    private Instant createdAt;
}
