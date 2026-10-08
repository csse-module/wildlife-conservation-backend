package com.wildlife.wildlife_conservationbackend.utility;

import com.wildlife.wildlife_conservationbackend.domain.TrackPoint;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PatrolDistanceCalculator {
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final long MAX_GAP_SECONDS = 120;
    private static final double MAX_ACCURACY_METERS = 100;

    public double calculate(List<TrackPoint> points) {
        double total = 0;
        for (int index = 1; index < points.size(); index++) {
            TrackPoint previous = points.get(index - 1);
            TrackPoint current = points.get(index);
            long gap = Duration.between(previous.getRecordedAt(), current.getRecordedAt()).toMillis();
            if (gap > 0 && gap <= MAX_GAP_SECONDS * 1000 && usable(previous) && usable(current)) {
                total += distance(previous, current);
            }
        }
        return Math.round(total * 100.0) / 100.0;
    }

    private boolean usable(TrackPoint point) {
        return point.getAccuracyMeters() == null || point.getAccuracyMeters() <= MAX_ACCURACY_METERS;
    }

    private double distance(TrackPoint first, TrackPoint second) {
        double latitudeDelta = Math.toRadians(second.getLatitude() - first.getLatitude());
        double longitudeDelta = Math.toRadians(second.getLongitude() - first.getLongitude());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(first.getLatitude())) * Math.cos(Math.toRadians(second.getLatitude()))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        return EARTH_RADIUS_METERS * 2 * Math.asin(Math.sqrt(Math.min(1, haversine)));
    }
}
