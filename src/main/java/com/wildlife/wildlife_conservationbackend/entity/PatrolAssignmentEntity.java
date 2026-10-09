package com.wildlife.wildlife_conservationbackend.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("patrol_assignments")
@CompoundIndex(name = "ranger_schedule_unique", def = "{'rangerId': 1, 'scheduledStartAt': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolAssignmentEntity {
    @MongoId
    private String id;
    @Indexed
    private String parkId;
    private String routeId;
    private String rangerId;
    private Instant scheduledStartAt;
    private Instant scheduledEndAt;
    private String assignedBy;
    private Instant createdAt;
    private String requestHash;
}
