package com.wildlife.wildlife_conservationbackend.utility;

import com.wildlife.wildlife_conservationbackend.domain.PatrolRequest;
import com.wildlife.wildlife_conservationbackend.domain.TrackPoint;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatrolValidator {
    private final Clock clock;

    public void validate(PatrolRequest request) {
        if (!request.getEndedAt().isAfter(request.getStartedAt())) {
            throw ApiException.invalid("endedAt must be after startedAt.");
        }
        if (request.getEndedAt().isAfter(clock.instant().plusSeconds(300))) {
            throw ApiException.invalid("A completed patrol cannot end in the future.");
        }
        Instant previous = request.getStartedAt();
        for (TrackPoint point : request.getTrackPoints()) {
            requireInPeriod(point.getRecordedAt(), request);
            if (point.getRecordedAt().isBefore(previous)) {
                throw ApiException.invalid("Track points must be ordered by recordedAt.");
            }
            previous = point.getRecordedAt();
        }
        Set<String> waypointIds = new HashSet<>();
        request.getWaypoints().forEach(waypoint -> {
            requireInPeriod(waypoint.getRecordedAt(), request);
            requireUnique(waypointIds, waypoint.getId());
        });
        Set<String> observationIds = new HashSet<>();
        request.getObservations().forEach(observation -> {
            requireInPeriod(observation.getObservedAt(), request);
            requireUnique(observationIds, observation.getId());
        });
    }

    private void requireInPeriod(Instant instant, PatrolRequest request) {
        if (instant.isBefore(request.getStartedAt()) || instant.isAfter(request.getEndedAt())) {
            throw ApiException.invalid("Recorded times must be within the patrol start and end times.");
        }
    }

    private void requireUnique(Set<String> ids, String id) {
        if (!ids.add(id)) {
            throw ApiException.invalid("Waypoint and observation IDs must be unique within their lists.");
        }
    }
}
