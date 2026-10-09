package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.enums.IncidentType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("incidents")
@CompoundIndex(name = "park_detectedAt", def = "{'parkId': 1, 'detectedAt': -1, '_id': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEntity {
    @MongoId
    private String id;

    @Indexed
    private String parkId;

    private String areaId;

    private String assignmentId;

    private IncidentType type;

    private Instant detectedAt;

    private Location location;

    private String description;

    @Indexed
    private String photoId;

    private String reportedBy;

    private Instant createdAt;

    private String requestHash;
}
