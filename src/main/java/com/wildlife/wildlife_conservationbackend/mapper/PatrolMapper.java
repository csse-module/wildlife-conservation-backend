package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.domain.PatrolObservation;
import com.wildlife.wildlife_conservationbackend.domain.PatrolRequest;
import com.wildlife.wildlife_conservationbackend.domain.TrackPoint;
import com.wildlife.wildlife_conservationbackend.domain.Waypoint;
import com.wildlife.wildlife_conservationbackend.dto.request.LocationRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolObservationRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.TrackPointRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.WaypointRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.PatrolEntity;
import org.springframework.stereotype.Component;

@Component
public class PatrolMapper {
    public PatrolRequest toRequest(PatrolRequestDTO dto) {
        return new PatrolRequest(dto.getAssignmentId(), dto.getStartedAt(), dto.getEndedAt(),
                dto.getTrackPoints().stream().map(this::toTrackPoint).toList(),
                dto.getWaypoints().stream().map(this::toWaypoint).toList(),
                dto.getObservations().stream().map(this::toObservation).toList());
    }

    public PatrolResponseDTO toResponse(PatrolEntity entity) {
        PatrolRequest submission = entity.getSubmission();
        return new PatrolResponseDTO(entity.getId(), entity.getAssignmentId(), entity.getRouteId(), entity.getParkId(),
                entity.getRangerId(), "COMPLETED", submission.getStartedAt(), submission.getEndedAt(),
                entity.getRecordedDistanceMeters(), submission.getWaypoints().size(), submission.getObservations().size(), entity.getCreatedAt(),
                submission.getTrackPoints(), submission.getWaypoints(), submission.getObservations());
    }

    private TrackPoint toTrackPoint(TrackPointRequestDTO dto) {
        return new TrackPoint(dto.getLatitude(), dto.getLongitude(), dto.getRecordedAt(), dto.getAccuracyMeters());
    }

    private Waypoint toWaypoint(WaypointRequestDTO dto) {
        return new Waypoint(dto.getId(), dto.getLabel(), dto.getRecordedAt(), toLocation(dto.getLocation()), dto.getNotes());
    }

    private PatrolObservation toObservation(PatrolObservationRequestDTO dto) {
        return new PatrolObservation(dto.getId(), dto.getText(), dto.getObservedAt(), toLocation(dto.getLocation()));
    }

    private Location toLocation(LocationRequestDTO dto) {
        return new Location(dto.getLatitude(), dto.getLongitude(), dto.getSource(), dto.getAccuracyMeters());
    }
}
