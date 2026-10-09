package com.wildlife.wildlife_conservationbackend.domain;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolRequest {
    private String assignmentId;
    private Instant startedAt;
    private Instant endedAt;
    private List<TrackPoint> trackPoints = List.of();
    private List<Waypoint> waypoints = List.of();
    private List<PatrolObservation> observations = List.of();
}
