package com.wildlife.wildlife_conservationbackend.domain;

import com.wildlife.wildlife_conservationbackend.enums.IncidentType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRequest {
    private String parkId;

    private String areaId;

    private String assignmentId;

    private IncidentType type;

    private Instant detectedAt;

    private Location location;

    private String description;

    private String photoId;
}
