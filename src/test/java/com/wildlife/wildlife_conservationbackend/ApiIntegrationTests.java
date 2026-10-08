package com.wildlife.wildlife_conservationbackend;

import com.wildlife.wildlife_conservationbackend.domain.GeoPoint;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolAssignmentEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolRouteEntity;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.AssignmentStatus;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolAssignmentQueryRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolAssignmentRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolQueryRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolRouteRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.utility.JwtTokenProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "wildlife.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
    "spring.data.mongodb.auto-index-creation=false",
    "spring.mongodb.uri=mongodb://localhost:27017/test?serverSelectionTimeoutMS=100&connectTimeoutMS=100"
})
@AutoConfigureMockMvc
class ApiIntegrationTests {
    private static final String ASSIGNMENT_ID = "00000000-0000-4000-8000-000000000001";
    private static final String PATROL_ID = "00000000-0000-4000-8000-000000000002";
    private static final String ASSIGNMENT_JSON = """
        {"routeId":"route-1","rangerId":"usr-ranger","scheduledStartAt":"2026-10-07T01:00:00Z",
         "scheduledEndAt":"2026-10-07T03:00:00Z"}
        """;

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenProvider tokens;
    @Autowired private PasswordEncoder encoder;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private UserRepository users;
    @MockitoBean private ParkRepository parks;
    @MockitoBean private PatrolRouteRepository routes;
    @MockitoBean private PatrolAssignmentRepository assignments;
    @MockitoBean private PatrolRepository patrols;
    @MockitoBean private MongoPageReader reader;
    @MockitoBean private PatrolAssignmentQueryRepository assignmentQueries;
    @MockitoBean private PatrolQueryRepository patrolQueries;

    @BeforeEach
    void listsReturnEmptyPages() {
        when(reader.find(any(Criteria.class), any(PageRequest.class), eq(ParkEntity.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(reader.find(any(Criteria.class), any(PageRequest.class), eq(UserEntity.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(reader.find(any(Criteria.class), any(PageRequest.class), eq(PatrolRouteEntity.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(assignmentQueries.find(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(patrolQueries.find(any(), any())).thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void loginIssuesTokenAndProfileDoesNotExposeHash() throws Exception {
        UserEntity ranger = user(Role.RANGER);
        when(users.findByNormalizedEmail("ranger@example.com")).thenReturn(Optional.of(ranger));
        var result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"RANGER@example.com\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("00"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andExpect(header().exists("X-Request-ID")).andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asString();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(ranger.getId()))
                .andExpect(jsonPath("$.data.email").value("ranger@example.com"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void invalidCredentialsAndDisabledAccountsReturnSameError() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"missing@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.errorCode").value("INVALID_CREDENTIALS"));
        var disabled = new UserEntity("disabled", "Disabled", "disabled@example.com", encoder.encode("test-password"),
                Role.RANGER, Set.of("park-1"), false);
        when(users.findByNormalizedEmail(disabled.getNormalizedEmail())).thenReturn(Optional.of(disabled));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"disabled@example.com\",\"password\":\"test-password\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.errorCode").value("INVALID_CREDENTIALS"));
        var withoutPassword = new UserEntity("no-password", "No Password", "no-password@example.com", null,
                Role.RANGER, Set.of("park-1"), true);
        when(users.findByNormalizedEmail(withoutPassword.getNormalizedEmail())).thenReturn(Optional.of(withoutPassword));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"no-password@example.com\",\"password\":\"unused-account-timing-check\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.errorCode").value("INVALID_CREDENTIALS"));
    }

    @Test
    void missingAndInvalidBearerTokensAreRejectedWithEnvelope() throws Exception {
        mvc.perform(get("/api/v1/parks")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("01")).andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(get("/api/v1/parks").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void disablingUserInvalidatesExistingToken() throws Exception {
        UserEntity ranger = user(Role.RANGER);
        String token = tokens.issue(ranger);
        when(users.findById(ranger.getId())).thenReturn(Optional.of(new UserEntity(ranger.getId(), ranger.getName(),
                ranger.getNormalizedEmail(), ranger.getPasswordHash(), ranger.getRole(), ranger.getParkIds(), false)));
        mvc.perform(get("/api/v1/parks").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void endpointRolesMatchRequestedMatrix(Role role) throws Exception {
        String bearer = "Bearer " + tokens.issue(user(role));
        boolean manager = role == Role.PARK_MANAGER;
        boolean patrolReader = manager || role == Role.RANGER;
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/parks").header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users").header("Authorization", bearer)).andExpect(status().is(manager ? 200 : 403));
        mvc.perform(get("/api/v1/patrol-routes").header("Authorization", bearer)).andExpect(status().is(patrolReader ? 200 : 403));
        mvc.perform(get("/api/v1/patrol-assignments").header("Authorization", bearer)).andExpect(status().is(patrolReader ? 200 : 403));
        mvc.perform(get("/api/v1/patrols").header("Authorization", bearer)).andExpect(status().is(patrolReader ? 200 : 403));
        mvc.perform(get("/api/v1/patrol-routes/missing").header("Authorization", bearer)).andExpect(status().is(patrolReader ? 404 : 403));
        mvc.perform(get("/api/v1/patrols/" + PATROL_ID).header("Authorization", bearer)).andExpect(status().is(patrolReader ? 404 : 403));
        mvc.perform(put("/api/v1/patrol-assignments/" + ASSIGNMENT_ID).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(ASSIGNMENT_JSON)).andExpect(status().is(manager ? 404 : 403));
        mvc.perform(put("/api/v1/patrols/" + PATROL_ID).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(patrolJson(6.0))).andExpect(status().is(role == Role.RANGER ? 404 : 403));
    }

    @Test
    void fieldValidationAndPaginationLimitsReturn400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fieldErrors.email").exists())
                .andExpect(jsonPath("$.error.fieldErrors.password").exists());
        String bearer = "Bearer " + tokens.issue(user(Role.RANGER));
        mvc.perform(get("/api/v1/patrols?size=101").header("Authorization", bearer)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/patrols?page=-1").header("Authorization", bearer)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/patrols?from=not-a-date").header("Authorization", bearer)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/patrols/" + PATROL_ID).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(patrolJson(100.0)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(patrols, assignments);
    }

    @Test
    void queryBeansBindDefaultsPagingStatusAndDates() throws Exception {
        String bearer = "Bearer " + tokens.issue(user(Role.PARK_MANAGER));
        mvc.perform(get("/api/v1/parks").header("Authorization", bearer)).andExpect(status().isOk());
        verify(reader).find(any(Criteria.class), eq(PageRequest.of(0, 20, Sort.by("name", "id"))), eq(ParkEntity.class));
        mvc.perform(get("/api/v1/parks?page=&size=").header("Authorization", bearer)).andExpect(status().isOk());
        verify(reader, times(2)).find(any(Criteria.class), eq(PageRequest.of(0, 20, Sort.by("name", "id"))), eq(ParkEntity.class));

        mvc.perform(get("/api/v1/patrol-assignments?parkId=park-1&status=COMPLETED&page=2&size=5")
                .header("Authorization", bearer)).andExpect(status().isOk());
        verify(assignmentQueries).find(any(Criteria.class), eq(AssignmentStatus.COMPLETED),
                eq(PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "scheduledStartAt", "_id"))));

        when(parks.findById("park-1")).thenReturn(Optional.of(new ParkEntity("park-1", "Park", "Asia/Colombo", List.of())));
        mvc.perform(get("/api/v1/patrols?parkId=park-1&routeId=route-1&from=2026-10-07&to=2026-10-07&page=1&size=10")
                .header("Authorization", bearer)).andExpect(status().isOk());
        var scope = ArgumentCaptor.forClass(Criteria.class);
        verify(patrolQueries).find(scope.capture(), eq(PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "endedAt", "_id"))));
        var filters = scope.getValue().getCriteriaObject();
        assertThat(filters).containsEntry("routeId", "route-1");
        assertThat(filters.get("endedAt", org.bson.Document.class))
                .containsEntry("$gte", Instant.parse("2026-10-06T18:30:00Z"))
                .containsEntry("$lt", Instant.parse("2026-10-07T18:30:00Z"));
    }

    @Test
    void nestedBeanValidationRejectsInvalidLocationsAndNullListElements() throws Exception {
        String bearer = "Bearer " + tokens.issue(user(Role.RANGER));
        String invalidLocation = patrolJson(6.0).replace("\"waypoints\":[]", """
            "waypoints":[{"id":"00000000-0000-4000-8000-000000000003","label":"Waterhole",
            "recordedAt":"2026-10-07T02:00:00Z","location":{"latitude":95.0,"longitude":81.5,"source":"GPS"}}]
            """);
        mvc.perform(put("/api/v1/patrols/" + PATROL_ID).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(invalidLocation))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorCode").value("VALIDATION_FAILED"));
        String nullObservation = patrolJson(6.0).replace("\"observations\":[]", "\"observations\":[null]");
        mvc.perform(put("/api/v1/patrols/" + PATROL_ID).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(nullObservation))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorCode").value("VALIDATION_FAILED"));
        String invalidObservation = patrolJson(6.0).replace("\"observations\":[]", """
            "observations":[{"id":"00000000-0000-4000-8000-000000000004","text":"",
            "observedAt":"2026-10-07T02:00:00Z","location":{"latitude":6.37,"longitude":81.5,"source":"GPS"}}]
            """);
        mvc.perform(put("/api/v1/patrols/" + PATROL_ID).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(invalidObservation))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorCode").value("VALIDATION_FAILED"));
        verifyNoInteractions(patrols, assignments);
    }

    @Test
    void parkBoundaryIsEnforced() throws Exception {
        String bearer = "Bearer " + tokens.issue(user(Role.RANGER));
        when(routes.findById("other-route")).thenReturn(Optional.of(new PatrolRouteEntity("other-route", "other-park",
                "area", "Other", 10, List.of(new GeoPoint(1.0, 1.0)))));
        mvc.perform(get("/api/v1/patrol-routes/other-route").header("Authorization", bearer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/patrol-assignments?parkId=other-park").header("Authorization", bearer))
                .andExpect(status().isForbidden());
    }

    @Test
    void storageFailureReturns503WithoutInternalMessage() throws Exception {
        when(users.findByNormalizedEmail("ranger@example.com"))
                .thenThrow(new DataAccessResourceFailureException("private connection details"));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ranger@example.com\",\"password\":\"test-password\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error.errorCode").value("STORAGE_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.errorDescription").value("The service is temporarily unavailable."));
    }

    @Test
    void serverOwnedFieldsAndInvalidUuidAreRejected() throws Exception {
        String bearer = "Bearer " + tokens.issue(user(Role.RANGER));
        String withActor = patrolJson(6.0).replace("\"trackPoints\"", "\"rangerId\":\"other-user\",\"trackPoints\"");
        mvc.perform(put("/api/v1/patrols/" + PATROL_ID).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(withActor)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/patrols/not-a-uuid").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(patrolJson(6.0))).andExpect(status().isBadRequest());
        verifyNoInteractions(patrols, assignments);
    }

    @Test
    void currentRoleOverridesRoleAtTokenIssuance() throws Exception {
        UserEntity manager = user(Role.PARK_MANAGER);
        String token = tokens.issue(manager);
        when(users.findById(manager.getId())).thenReturn(Optional.of(new UserEntity(manager.getId(), manager.getName(),
                manager.getNormalizedEmail(), manager.getPasswordHash(), Role.COMMUNITY_MEMBER, manager.getParkIds(), true)));
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void assignmentResponsesUseGeneratorForCreationRetryAndConflict() throws Exception {
        String bearer = "Bearer " + tokens.issue(user(Role.PARK_MANAGER));
        user(Role.RANGER);
        when(routes.findById("route-1")).thenReturn(Optional.of(new PatrolRouteEntity("route-1", "park-1",
                "area", "Route", 100, List.of())));
        var stored = new AtomicReference<PatrolAssignmentEntity>();
        when(assignments.findById(ASSIGNMENT_ID)).thenAnswer(call -> Optional.ofNullable(stored.get()));
        when(assignments.insert(any(PatrolAssignmentEntity.class))).thenAnswer(call -> {
            PatrolAssignmentEntity assignment = call.getArgument(0);
            stored.set(assignment);
            return assignment;
        });
        String path = "/api/v1/patrol-assignments/" + ASSIGNMENT_ID;
        mvc.perform(put(path).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(ASSIGNMENT_JSON))
                .andExpect(status().isCreated()).andExpect(header().string("Location", path))
                .andExpect(jsonPath("$.status").value("00")).andExpect(jsonPath("$.description").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(ASSIGNMENT_ID))
                .andExpect(jsonPath("$.error.errorCode").value("00"));
        mvc.perform(put(path).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(ASSIGNMENT_JSON))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.data.id").value(ASSIGNMENT_ID));
        String changed = ASSIGNMENT_JSON.replace("2026-10-07T03:00:00Z", "2026-10-07T04:00:00Z");
        mvc.perform(put(path).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(changed))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value("01"))
                .andExpect(jsonPath("$.error.errorCode").value("IDEMPOTENCY_CONFLICT"));
        verify(assignments).insert(any(PatrolAssignmentEntity.class));
    }

    @Test
    void patrolResponsesUseGeneratorForCreationRetryAndConflict() throws Exception {
        UserEntity ranger = user(Role.RANGER);
        String bearer = "Bearer " + tokens.issue(ranger);
        Instant start = Instant.parse("2026-10-07T01:00:00Z");
        when(assignments.findById(ASSIGNMENT_ID)).thenReturn(Optional.of(new PatrolAssignmentEntity(ASSIGNMENT_ID,
                "park-1", "route-1", ranger.getId(), start, start.plusSeconds(7200), "manager", start, "assignment-hash")));
        var stored = new AtomicReference<PatrolEntity>();
        when(patrols.findById(PATROL_ID)).thenAnswer(call -> Optional.ofNullable(stored.get()));
        when(patrols.insert(any(PatrolEntity.class))).thenAnswer(call -> {
            PatrolEntity patrol = call.getArgument(0);
            stored.set(patrol);
            return patrol;
        });
        String path = "/api/v1/patrols/" + PATROL_ID;
        String payload = patrolJson(6.0).replace("\"waypoints\":[]", """
            "waypoints":[{"id":"00000000-0000-4000-8000-000000000003","label":"Waterhole",
            "recordedAt":"2026-10-07T02:00:00Z","notes":"Elephant tracks",
            "location":{"latitude":6.37,"longitude":81.5,"source":"MANUAL"}}]
            """).replace("\"observations\":[]", """
            "observations":[{"id":"00000000-0000-4000-8000-000000000004","text":"Elephants",
            "observedAt":"2026-10-07T02:00:00Z",
            "location":{"latitude":6.37,"longitude":81.5,"source":"GPS","accuracyMeters":8.0}}]
            """);
        mvc.perform(put(path).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated()).andExpect(header().string("Location", path))
                .andExpect(jsonPath("$.status").value("00")).andExpect(jsonPath("$.description").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(PATROL_ID))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.waypointCount").value(1))
                .andExpect(jsonPath("$.data.waypoints[0].label").value("Waterhole"))
                .andExpect(jsonPath("$.data.waypoints[0].notes").value("Elephant tracks"))
                .andExpect(jsonPath("$.data.waypoints[0].location.source").value("MANUAL"))
                .andExpect(jsonPath("$.data.observationCount").value(1))
                .andExpect(jsonPath("$.data.observations[0].text").value("Elephants"))
                .andExpect(jsonPath("$.data.observations[0].location.accuracyMeters").value(8.0))
                .andExpect(jsonPath("$.error.errorCode").value("00"));
        mvc.perform(put(path).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.data.id").value(PATROL_ID));
        String changed = payload.replace("\"latitude\":6.0", "\"latitude\":6.1");
        mvc.perform(put(path).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(changed))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value("01"))
                .andExpect(jsonPath("$.error.errorCode").value("IDEMPOTENCY_CONFLICT"));
        verify(patrols).insert(any(PatrolEntity.class));
    }

    private UserEntity user(Role role) {
        String id = "usr-" + role.name().toLowerCase(java.util.Locale.ROOT);
        var user = new UserEntity(id, "Test User", "ranger@example.com", encoder.encode("test-password"), role, Set.of("park-1"), true);
        when(users.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    private String patrolJson(double latitude) {
        return """
            {"assignmentId":"%s","startedAt":"2026-10-07T01:00:00Z","endedAt":"2026-10-07T03:00:00Z",
             "trackPoints":[{"latitude":%s,"longitude":81.5,"recordedAt":"2026-10-07T02:00:00Z"}],
             "waypoints":[],"observations":[]}
            """.formatted(ASSIGNMENT_ID, latitude);
    }
}
