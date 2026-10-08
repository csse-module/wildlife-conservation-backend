package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.domain.PatrolAssignmentRequest;
import com.wildlife.wildlife_conservationbackend.domain.PatrolObservation;
import com.wildlife.wildlife_conservationbackend.domain.PatrolRequest;
import com.wildlife.wildlife_conservationbackend.domain.TrackPoint;
import com.wildlife.wildlife_conservationbackend.domain.Waypoint;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolQuery;
import com.wildlife.wildlife_conservationbackend.entity.PatrolAssignmentEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolRouteEntity;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.LocationSource;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.ParkMapper;
import com.wildlife.wildlife_conservationbackend.mapper.PatrolAssignmentMapper;
import com.wildlife.wildlife_conservationbackend.mapper.PatrolMapper;
import com.wildlife.wildlife_conservationbackend.mapper.PatrolRouteMapper;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolAssignmentQueryRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolAssignmentRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolQueryRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolRouteRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.ParkServiceImpl;
import com.wildlife.wildlife_conservationbackend.service.impl.PatrolServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.PatrolDateFilter;
import com.wildlife.wildlife_conservationbackend.utility.PatrolDistanceCalculator;
import com.wildlife.wildlife_conservationbackend.utility.PatrolValidator;
import com.wildlife.wildlife_conservationbackend.utility.RequestFingerprint;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatrolWorkflowTests {
    private static final Instant START = Instant.parse("2026-10-07T01:00:00Z");
    private static final Instant END = START.plusSeconds(3600);
    private static final CurrentUser MANAGER = new CurrentUser("manager", Role.PARK_MANAGER, Set.of("park"));
    private static final CurrentUser RANGER = new CurrentUser("ranger", Role.RANGER, Set.of("park"));
    @Mock private PatrolAssignmentRepository assignments;
    @Mock private PatrolAssignmentQueryRepository assignmentQueries;
    @Mock private PatrolRouteRepository routes;
    @Mock private UserRepository users;
    @Mock private PatrolRepository patrols;
    @Mock private PatrolQueryRepository patrolQueries;
    @Mock private MongoPageReader reader;
    @Mock private ParkRepository parks;
    @Mock private PatrolDateFilter dates;
    private PatrolServiceImpl patrolService;
    private RequestFingerprint fingerprint;

    @BeforeEach
    void services() {
        Clock clock = Clock.fixed(END.plusSeconds(600), ZoneOffset.UTC);
        fingerprint = new RequestFingerprint(JsonMapper.builder().findAndAddModules().build());
        ResponseGenerator responseGenerator = new ResponseGenerator();
        var parkService = new ParkServiceImpl(reader, new ParkMapper(), responseGenerator, parks);
        patrolService = new PatrolServiceImpl(patrols, assignments, routes, users, assignmentQueries, patrolQueries,
                reader, parkService, new PatrolMapper(), new PatrolAssignmentMapper(), new PatrolRouteMapper(),
                new PatrolValidator(clock), new PatrolDistanceCalculator(), dates, fingerprint, clock, responseGenerator);
    }

    @Test
    void assignmentCreatesThenExactRetryReturnsExisting() {
        var request = new PatrolAssignmentRequest("route", "ranger", START, END);
        var existing = assignment(request);
        when(assignments.findById("assignment")).thenReturn(Optional.empty()).thenReturn(Optional.of(existing));
        when(routes.findById("route")).thenReturn(Optional.of(new PatrolRouteEntity("route", "park", "area", "Route", 100, List.of())));
        when(users.findById("ranger")).thenReturn(Optional.of(new UserEntity("ranger", "Ranger", "ranger@example.com",
                "hash", Role.RANGER, Set.of("park"), true)));
        when(assignments.insert(any(PatrolAssignmentEntity.class))).thenAnswer(call -> call.getArgument(0));
        assertThat(patrolService.assign(MANAGER, "assignment", request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(patrolService.assign(MANAGER, "assignment", request).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(assignments).insert(any(PatrolAssignmentEntity.class));
    }

    @Test
    void assignmentRejectsChangedPayloadAndAnotherManagerReusingId() {
        var request = new PatrolAssignmentRequest("route", "ranger", START, END);
        when(assignments.findById("assignment")).thenReturn(Optional.of(assignment(request)));
        var changed = new PatrolAssignmentRequest("route", "ranger", START, END.plusSeconds(1));
        assertThatThrownBy(() -> patrolService.assign(MANAGER, "assignment", changed))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("IDEMPOTENCY_CONFLICT");
        var other = new CurrentUser("other-manager", Role.PARK_MANAGER, Set.of("park"));
        assertThatThrownBy(() -> patrolService.assign(other, "assignment", request)).isInstanceOf(ApiException.class);
        verify(assignments, never()).insert(any(PatrolAssignmentEntity.class));
    }

    @Test
    void assignmentDuplicateScheduleIs409() {
        var request = new PatrolAssignmentRequest("route", "ranger", START, END);
        when(routes.findById("route")).thenReturn(Optional.of(new PatrolRouteEntity("route", "park", "area", "Route", 100, List.of())));
        when(users.findById("ranger")).thenReturn(Optional.of(new UserEntity("ranger", "Ranger", "ranger@example.com",
                "hash", Role.RANGER, Set.of("park"), true)));
        when(assignments.insert(any(PatrolAssignmentEntity.class))).thenThrow(new DuplicateKeyException("schedule"));
        assertThatThrownBy(() -> patrolService.assign(MANAGER, "assignment", request))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ASSIGNMENT_SCHEDULE_CONFLICT");
    }

    @Test
    void rangerCannotCompleteSomeoneElsesAssignment() {
        var request = patrolRequest();
        when(assignments.findById("assignment")).thenReturn(Optional.of(new PatrolAssignmentEntity("assignment", "park", "route",
                "different-ranger", START, END, "manager", START, "hash")));
        assertThatThrownBy(() -> patrolService.complete(RANGER, "patrol", request)).isInstanceOf(ApiException.class);
        verify(patrols, never()).insert(any(PatrolEntity.class));
    }

    @Test
    void patrolCreatesAndExactRetryDoesNotWriteTwice() {
        var request = patrolRequest();
        var saved = patrol(request);
        when(patrols.findById("patrol")).thenReturn(Optional.empty()).thenReturn(Optional.of(saved));
        when(assignments.findById("assignment")).thenReturn(Optional.of(assignment(new PatrolAssignmentRequest("route", "ranger", START, END))));
        when(patrols.insert(any(PatrolEntity.class))).thenAnswer(call -> call.getArgument(0));
        assertThat(patrolService.complete(RANGER, "patrol", request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(patrolService.complete(RANGER, "patrol", request).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(patrols).insert(any(PatrolEntity.class));
    }

    @Test
    void concurrentPatrolRetryReturnsWinner() {
        var request = patrolRequest();
        when(patrols.findById("patrol")).thenReturn(Optional.empty()).thenReturn(Optional.of(patrol(request)));
        when(assignments.findById("assignment")).thenReturn(Optional.of(assignment(new PatrolAssignmentRequest("route", "ranger", START, END))));
        when(patrols.insert(any(PatrolEntity.class))).thenThrow(new DuplicateKeyException("race"));
        assertThat(patrolService.complete(RANGER, "patrol", request).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void concurrentAssignmentRetryReturnsWinnerWithoutCreationHeader() {
        var request = new PatrolAssignmentRequest("route", "ranger", START, END);
        when(assignments.findById("assignment")).thenReturn(Optional.empty()).thenReturn(Optional.of(assignment(request)));
        when(routes.findById("route")).thenReturn(Optional.of(new PatrolRouteEntity("route", "park", "area", "Route", 100, List.of())));
        when(users.findById("ranger")).thenReturn(Optional.of(new UserEntity("ranger", "Ranger", "ranger@example.com",
                "hash", Role.RANGER, Set.of("park"), true)));
        when(assignments.insert(any(PatrolAssignmentEntity.class))).thenThrow(new DuplicateKeyException("race"));

        var response = patrolService.assign(MANAGER, "assignment", request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getLocation()).isNull();
        assertThat(response.getBody().getData().getId()).isEqualTo("assignment");
        verify(assignments).insert(any(PatrolAssignmentEntity.class));
    }

    @Test
    void onePatrolPerAssignmentAndImmutableSnapshotAreEnforced() {
        var request = patrolRequest();
        when(assignments.findById("assignment")).thenReturn(Optional.of(assignment(new PatrolAssignmentRequest("route", "ranger", START, END))));
        when(patrols.insert(any(PatrolEntity.class))).thenThrow(new DuplicateKeyException("assignment"));
        assertThatThrownBy(() -> patrolService.complete(RANGER, "patrol", request))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ASSIGNMENT_ALREADY_USED");
        when(patrols.findById("patrol")).thenReturn(Optional.of(patrol(request)));
        var changed = new PatrolRequest("assignment", START, END.plusSeconds(1), List.of(), List.of(), List.of());
        assertThatThrownBy(() -> patrolService.complete(RANGER, "patrol", changed))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("IDEMPOTENCY_CONFLICT");
    }

    @Test
    void queriesScopeRangersToOwnRecordsAndManagersToAssignedParks() {
        when(patrolQueries.find(any(Criteria.class), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));
        var query = new PatrolQuery(null, null, null, null);
        var page = new PageQuery(0, 20);
        patrolService.listPatrols(RANGER, query, page);
        patrolService.listPatrols(MANAGER, query, page);
        var scope = ArgumentCaptor.forClass(Criteria.class);
        verify(patrolQueries, times(2)).find(scope.capture(), any(PageRequest.class));
        assertThat(scope.getAllValues().get(0).getCriteriaObject()).containsEntry("rangerId", RANGER.getId());
        assertThat(scope.getAllValues().get(1).getCriteriaObject()).doesNotContainKey("rangerId");
        assertThatThrownBy(() -> patrolService.listPatrols(RANGER, new PatrolQuery("other", null, null, null), page))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void requestFingerprintsRemainCompatibleWithStoredRecords() {
        var assignmentRequest = new PatrolAssignmentRequest("route", "ranger", START, END);
        var location = new Location(6.37, 81.5, LocationSource.GPS, 8.0);
        var patrolRequest = new PatrolRequest("assignment", START, END,
                List.of(new TrackPoint(6.37, 81.5, START, 8.0)),
                List.of(new Waypoint("waypoint", "Waterhole", START, location, null)),
                List.of(new PatrolObservation("observation", "Elephants", START, location)));
        // These hashes were captured from the record models before their conversion to Java beans.
        assertThat(fingerprint.of(assignmentRequest))
                .isEqualTo("80268f1e09ee4d68b6941fb318922013f15564fb7bc098ee4038b6b4d8faad46");
        assertThat(fingerprint.of(patrolRequest))
                .isEqualTo("84ee682d3280c0bfb1f4759e8812dafc2a4de3f167b079d31741187a98ead3e1");
    }

    private PatrolAssignmentEntity assignment(PatrolAssignmentRequest request) {
        return new PatrolAssignmentEntity("assignment", "park", "route", "ranger", START, END, "manager", START, fingerprint.of(request));
    }

    private PatrolRequest patrolRequest() {
        return new PatrolRequest("assignment", START, END, List.of(), List.of(), List.of());
    }

    private PatrolEntity patrol(PatrolRequest request) {
        return new PatrolEntity("patrol", "assignment", "route", "park", "ranger", END, request, 0, END, fingerprint.of(request));
    }
}
