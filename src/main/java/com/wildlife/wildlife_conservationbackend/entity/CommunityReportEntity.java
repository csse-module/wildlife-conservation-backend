package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("community_reports")
@CompoundIndex(name = "park_occurredAt", def = "{'parkId': 1, 'occurredAt': -1, '_id': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityReportEntity {
    @MongoId
    private String id;

    @Indexed
    private String parkId;

    private String areaId;

    private CommunityReportType type;

    private String species;

    private String village;

    private Instant occurredAt;

    private Location location;

    private String description;

    private String cropDetails;

    @Indexed
    private String photoId;

    private String reportedBy;

    private Instant createdAt;

    private String requestHash;
}
