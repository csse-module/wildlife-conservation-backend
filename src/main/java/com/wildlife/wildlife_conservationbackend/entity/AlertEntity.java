package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.enums.AlertStatus;
import com.wildlife.wildlife_conservationbackend.enums.RiskLevel;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("alerts")
@CompoundIndex(name = "park_detectedAt", def = "{'parkId': 1, 'detectedAt': -1, '_id': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEntity {
    @MongoId
    private String id;

    @Indexed
    private String parkId;

    private String areaId;

    private String animal;

    private String collarId;

    private RiskLevel riskLevel;

    private Location location;

    private Instant locationUpdatedAt;

    private Instant detectedAt;

    private AlertStatus status;

    private String createdBy;

    private Instant createdAt;

    private String requestHash;

    private String assignedOfficerId;

    private Instant acceptedAt;

    private List<AlertDeclineEntity> declines;

    private List<AlertSupportEntity> supportRequests;

    private AlertResponseEntity response;

    private String responseHash;

    private Instant resolvedAt;
}
