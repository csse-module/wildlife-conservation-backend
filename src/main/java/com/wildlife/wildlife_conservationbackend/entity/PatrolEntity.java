package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.domain.PatrolRequest;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("patrols")
@CompoundIndexes({
    @CompoundIndex(name = "park_completed", def = "{'parkId': 1, 'endedAt': -1, '_id': -1}"),
    @CompoundIndex(name = "ranger_completed", def = "{'rangerId': 1, 'endedAt': -1, '_id': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolEntity {
    @MongoId
    private String id;
    @Indexed(unique = true)
    private String assignmentId;
    private String routeId;
    private String parkId;
    private String rangerId;
    private Instant endedAt;
    private PatrolRequest submission;
    private double recordedDistanceMeters;
    private Instant createdAt;
    private String requestHash;
}
