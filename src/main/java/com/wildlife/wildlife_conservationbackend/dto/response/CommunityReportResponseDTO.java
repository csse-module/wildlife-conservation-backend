package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityReportResponseDTO {
    private String id;

    private String parkId;

    private String areaId;

    private CommunityReportType type;

    private String species;

    private String village;

    private Instant occurredAt;

    private Location location;

    private String description;

    private String cropDetails;

    private String photoId;

    private String reportedBy;

    private String status;

    private Instant createdAt;
}
