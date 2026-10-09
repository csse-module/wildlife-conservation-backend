package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.domain.PatrolObservation;
import com.wildlife.wildlife_conservationbackend.domain.TrackPoint;
import com.wildlife.wildlife_conservationbackend.domain.Waypoint;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolResponseDTO {
    private String id;
    private String assignmentId;
    private String routeId;
    private String parkId;
    private String rangerId;
    private String status;
    private Instant startedAt;
    private Instant endedAt;
    private double recordedDistanceMeters;
    private int waypointCount;
    private int observationCount;
    private Instant createdAt;
    private List<TrackPoint> trackPoints;
    private List<Waypoint> waypoints;
    private List<PatrolObservation> observations;
}
