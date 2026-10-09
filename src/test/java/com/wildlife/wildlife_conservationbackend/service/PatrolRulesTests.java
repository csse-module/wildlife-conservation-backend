package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.domain.PatrolRequest;
import com.wildlife.wildlife_conservationbackend.domain.TrackPoint;
import com.wildlife.wildlife_conservationbackend.domain.Waypoint;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolQuery;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.enums.LocationSource;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.utility.PatrolDateFilter;
import com.wildlife.wildlife_conservationbackend.utility.PatrolDistanceCalculator;
import com.wildlife.wildlife_conservationbackend.utility.PatrolValidator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatrolRulesTests {
    private static final Instant START = Instant.parse("2026-10-07T01:00:00Z");
    private static final Instant END = START.plusSeconds(3600);
    private final PatrolValidator validator = new PatrolValidator(Clock.fixed(END, ZoneOffset.UTC));
    private final PatrolDistanceCalculator calculator = new PatrolDistanceCalculator();

    @Test
    void distanceUsesContinuousAccurateGpsSegments() {
        var first = new TrackPoint(0.0, 0.0, START, 5.0);
        var second = new TrackPoint(0.0, 0.001, START.plusSeconds(60), 5.0);
        assertThat(calculator.calculate(List.of(first, second))).isCloseTo(111.19, within(0.02));
        var afterGap = new TrackPoint(0.0, 0.002, START.plusSeconds(181), 5.0);
        var inaccurate = new TrackPoint(0.0, 0.003, START.plusSeconds(190), 101.0);
        assertThat(calculator.calculate(List.of(first, second, afterGap, inaccurate))).isCloseTo(111.19, within(0.02));
        assertThat(calculator.calculate(List.of())).isZero();
    }

    @Test
    void patrolRejectsInvalidPeriodUnorderedTrackAndFutureCompletion() {
        assertThatThrownBy(() -> validator.validate(new PatrolRequest("assignment", START, START, List.of(), List.of(), List.of())))
                .isInstanceOf(ApiException.class);
        var points = List.of(new TrackPoint(1.0, 1.0, START.plusSeconds(10), null), new TrackPoint(1.0, 1.0, START, null));
        assertThatThrownBy(() -> validator.validate(new PatrolRequest("assignment", START, END, points, List.of(), List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validator.validate(new PatrolRequest("assignment", START, END.plusSeconds(301), List.of(), List.of(), List.of())))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void waypointsMustBeInPeriodAndHaveUniqueIds() {
        var location = new Location(6.0, 81.0, LocationSource.MANUAL, null);
        var waypoint = new Waypoint("id", "Waypoint", START, location, null);
        assertThatThrownBy(() -> validator.validate(new PatrolRequest("assignment", START, END, List.of(), List.of(waypoint, waypoint), List.of())))
                .isInstanceOf(ApiException.class);
        var outside = new Waypoint("id", "Waypoint", START.minusSeconds(1), location, null);
        assertThatThrownBy(() -> validator.validate(new PatrolRequest("assignment", START, END, List.of(), List.of(outside), List.of())))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void dateFilterUsesInclusiveParkLocalDatesAndExclusiveUtcEnd() {
        var parks = mock(ParkRepository.class);
        when(parks.findById("park")).thenReturn(Optional.of(new ParkEntity("park", "Park", "Asia/Colombo", List.of())));
        var dates = new PatrolDateFilter(parks);
        Criteria scope = new Criteria();
        var day = LocalDate.parse("2026-10-07");
        dates.apply(scope, new PatrolQuery("park", null, day, day));
        var interval = (org.bson.Document) scope.getCriteriaObject().get("endedAt");
        assertThat(interval.get("$gte")).isEqualTo(Instant.parse("2026-10-06T18:30:00Z"));
        assertThat(interval.get("$lt")).isEqualTo(Instant.parse("2026-10-07T18:30:00Z"));
        assertThatThrownBy(() -> dates.apply(new Criteria(), new PatrolQuery(null, null, day, day))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> dates.apply(new Criteria(), new PatrolQuery("park", null, day, day.plusDays(92))))
                .isInstanceOf(ApiException.class);
    }
}
