package com.wildlife.wildlife_conservationbackend;

import com.wildlife.wildlife_conservationbackend.domain.AnalyticsFacts;
import com.wildlife.wildlife_conservationbackend.domain.DateRange;
import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.entity.AlertEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraImageEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraTrapEntity;
import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import com.wildlife.wildlife_conservationbackend.entity.IncidentEntity;
import com.wildlife.wildlife_conservationbackend.entity.MediaEntity;
import com.wildlife.wildlife_conservationbackend.entity.ParkArea;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.entity.ReportEntity;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.LocationSource;
import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.repository.AlertRepository;
import com.wildlife.wildlife_conservationbackend.repository.AlertTransitionRepository;
import com.wildlife.wildlife_conservationbackend.repository.AnalyticsRepository;
import com.wildlife.wildlife_conservationbackend.repository.CameraImageRepository;
import com.wildlife.wildlife_conservationbackend.repository.CameraImageTransitionRepository;
import com.wildlife.wildlife_conservationbackend.repository.CameraTrapRepository;
import com.wildlife.wildlife_conservationbackend.repository.CommunityReportRepository;
import com.wildlife.wildlife_conservationbackend.repository.IncidentRepository;
import com.wildlife.wildlife_conservationbackend.repository.MediaAccessRepository;
import com.wildlife.wildlife_conservationbackend.repository.MediaRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.repository.ReportRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.utility.JwtTokenProvider;
import com.wildlife.wildlife_conservationbackend.utility.MediaStorage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "wildlife.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
    "spring.data.mongodb.auto-index-creation=false",
    "spring.mongodb.uri=mongodb://localhost:27017/test?serverSelectionTimeoutMS=100&connectTimeoutMS=100"
})
@AutoConfigureMockMvc
class NewFeatureApiTests {
    private static final String ID = "10000000-0000-4000-8000-000000000001";
    private static final String PHOTO = "10000000-0000-4000-8000-000000000002";
    private static final String LOCATION = "{\"latitude\":6.37,\"longitude\":81.5,\"source\":\"GPS\",\"accuracyMeters\":8}";
    private static final String INCIDENT = "{\"parkId\":\"park\",\"areaId\":\"area\",\"type\":\"SNARE\","
            + "\"detectedAt\":\"2026-10-07T01:00:00Z\",\"location\":" + LOCATION + ",\"description\":\"Snare found\",\"photoId\":\"" + PHOTO + "\"}";
    private static final String ALERT = "{\"parkId\":\"park\",\"areaId\":\"area\",\"animal\":\"Elephant\",\"collarId\":\"ELE-1\","
            + "\"riskLevel\":\"HIGH\",\"location\":" + LOCATION + ",\"locationUpdatedAt\":\"2026-10-07T01:00:00Z\",\"detectedAt\":\"2026-10-07T01:00:00Z\"}";
    private static final String REPORT = "{\"parkId\":\"park\",\"reportType\":\"MONTHLY_CONSERVATION\","
            + "\"from\":\"2026-10-01\",\"to\":\"2026-10-07\",\"sections\":[\"PATROL_COVERAGE\"]}";
    private static final String COMMUNITY = "{\"parkId\":\"park\",\"areaId\":\"area\",\"type\":\"CROP_DAMAGE\","
            + "\"village\":\"Village\",\"occurredAt\":\"2026-10-07T01:00:00Z\",\"description\":\"Damaged paddy\",\"cropDetails\":\"Paddy\"}";
    private static final String CAMERA = "{\"cameraTrapId\":\"camera\",\"mediaId\":\"" + PHOTO + "\",\"capturedAt\":\"2026-10-07T01:00:00Z\"}";
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenProvider tokens;
    @MockitoBean private UserRepository users;
    @MockitoBean private ParkRepository parks;
    @MockitoBean private MediaRepository media;
    @MockitoBean private MediaAccessRepository mediaAccess;
    @MockitoBean private MediaStorage storage;
    @MockitoBean private IncidentRepository incidents;
    @MockitoBean private CommunityReportRepository community;
    @MockitoBean private AlertRepository alerts;
    @MockitoBean private AlertTransitionRepository alertTransitions;
    @MockitoBean private CameraTrapRepository cameras;
    @MockitoBean private CameraImageRepository images;
    @MockitoBean private CameraImageTransitionRepository imageTransitions;
    @MockitoBean private ReportRepository reports;
    @MockitoBean private AnalyticsRepository analytics;
    @MockitoBean private MongoPageReader reader;
    private byte[] png;

    @BeforeEach
    void fixtures() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
        png = output.toByteArray();
        when(parks.findById("park")).thenReturn(Optional.of(new ParkEntity("park", "Park", "Asia/Colombo", List.of(new ParkArea("area", "Area")))));
        when(parks.existsById("park")).thenReturn(true);
        when(cameras.findById("camera")).thenReturn(Optional.of(new CameraTrapEntity("camera", "park", "area", "Camera",
                new Location(6.37, 81.5, LocationSource.GPS, 8.0))));
        when(reader.find(any(Criteria.class), any(PageRequest.class), any())).thenAnswer(call -> new PageImpl<>(List.of()));
        when(analytics.summarize(eq("park"), any(DateRange.class))).thenReturn(new AnalyticsFacts(Map.of(), Map.of(), Map.of(), 0, 0, 0, 0, 0));
        when(media.insert(any(MediaEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(incidents.insert(any(IncidentEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(community.insert(any(CommunityReportEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(alerts.insert(any(AlertEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(images.insert(any(CameraImageEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(reports.insert(any(ReportEntity.class))).thenAnswer(call -> call.getArgument(0));
        when(storage.store(any(), any(), any())).thenReturn("0".repeat(64) + ".png");
    }

    @ParameterizedTest(name = "{0} {1}: {3} allowed={4}")
    @MethodSource("permissions")
    void allNewEndpointsEnforceTheRequestedRoles(String method, String path, String payload, Role role, boolean allowed) throws Exception {
        String authorization = authenticate(role);
        MediaCategory category = role == Role.RANGER ? MediaCategory.INCIDENT : MediaCategory.CAMERA_TRAP;
        when(media.findById(PHOTO)).thenReturn(Optional.of(MediaEntity.builder().id(PHOTO).ownerId(role.name())
                .parkId("park").category(category).contentType("image/png").sizeBytes(png.length).build()));
        var result = mvc.perform(request(method, path, payload, role).header("Authorization", authorization)).andReturn();
        if (allowed) {
            assertThat(result.getResponse().getStatus()).isIn(200, 201, 404);
        } else {
            assertThat(result.getResponse().getStatus()).isEqualTo(403);
            assertThat(result.getResponse().getContentAsString()).contains("ACCESS_DENIED");
        }
    }

    @ParameterizedTest(name = "Unauthenticated {0} {1}")
    @MethodSource("endpoints")
    void allNewEndpointsRequireAuthentication(String method, String path, String payload, String allowedRoles) throws Exception {
        mvc.perform(request(method, path, payload, Role.RANGER)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void nestedIncidentValidationAndUnknownFieldsAreRejectedBeforePersistence() throws Exception {
        String auth = authenticate(Role.RANGER);
        mvc.perform(put("/api/v1/incidents/" + ID).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(INCIDENT.replace("6.37", "96.37"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors['location.latitude']").exists());
        mvc.perform(put("/api/v1/incidents/" + ID).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content(INCIDENT.replace("\"description\":", "\"reportedBy\":\"someone-else\",\"description\":")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void multipartDtoRequiresFileAndBinaryMediaDoesNotExposeStorageDetails() throws Exception {
        String auth = authenticate(Role.RANGER);
        mvc.perform(multipart("/api/v1/media/" + ID).with(request -> { request.setMethod("PUT"); return request; })
                .param("parkId", "park").param("category", "INCIDENT").header("Authorization", auth))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fieldErrors.file").exists());
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png));
        when(media.findById(ID)).thenReturn(Optional.of(MediaEntity.builder().id(ID).ownerId(Role.RANGER.name())
                .parkId("park").category(MediaCategory.INCIDENT).contentType("image/png").sizeBytes(png.length)
                .storageKey(checksum + ".png").sha256(checksum).build()));
        when(storage.read(checksum + ".png")).thenReturn(png);
        mvc.perform(get("/api/v1/media/" + ID + "/content").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(png)).andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void otherParkIsRejectedAndMissingRequiredQueryIsBadRequest() throws Exception {
        String auth = authenticate(Role.PARK_MANAGER);
        mvc.perform(get("/api/v1/analytics/summary?parkId=other&from=2026-10-01&to=2026-10-07").header("Authorization", auth))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.errorCode").value("PARK_ACCESS_DENIED"));
        mvc.perform(get("/api/v1/camera-traps").header("Authorization", auth))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/analytics/summary?parkId=park&from=2026-01-01&to=2026-10-07").header("Authorization", auth))
                .andExpect(status().isBadRequest());
    }

    private String authenticate(Role role) {
        var user = new UserEntity(role.name(), role.name(), role.name() + "@example.com", "hash", role, Set.of("park"), true);
        when(users.findById(role.name())).thenReturn(Optional.of(user));
        return "Bearer " + tokens.issue(user);
    }

    private AbstractMockHttpServletRequestBuilder<?> request(String method, String path, String payload, Role role) {
        String url = "/api/v1" + path.replace("{id}", ID);
        if ("UPLOAD".equals(method)) {
            String category = switch (role) {
                case RANGER -> "INCIDENT";
                case LIAISON_OFFICER -> "ALERT_RESPONSE";
                case COMMUNITY_MEMBER -> "COMMUNITY_REPORT";
                case PARK_MANAGER, RESEARCHER -> "CAMERA_TRAP";
            };
            return multipart(url).file(new MockMultipartFile("file", "image.png", "image/png", png))
                    .param("parkId", "park").param("category", category)
                    .with(request -> { request.setMethod("PUT"); return request; });
        }
        return switch (method) {
            case "GET" -> get(url);
            case "POST" -> post(url).contentType(MediaType.APPLICATION_JSON).content(payload);
            case "PUT" -> put(url).contentType(MediaType.APPLICATION_JSON).content(payload);
            default -> throw new IllegalArgumentException("Unsupported test method");
        };
    }

    static Stream<Arguments> permissions() {
        return endpoints().flatMap(endpoint -> {
            Object[] values = endpoint.get();
            Set<String> allowed = Set.of(((String) values[3]).split(","));
            return Arrays.stream(Role.values()).map(role -> Arguments.of(values[0], values[1], values[2], role, allowed.contains(role.name())));
        });
    }

    static Stream<Arguments> endpoints() {
        return Stream.of(
            Arguments.of("UPLOAD", "/media/{id}", "", "PARK_MANAGER,RANGER,LIAISON_OFFICER,RESEARCHER,COMMUNITY_MEMBER"),
            Arguments.of("GET", "/media/{id}/content", "", "PARK_MANAGER,RANGER,LIAISON_OFFICER,RESEARCHER,COMMUNITY_MEMBER"),
            Arguments.of("PUT", "/incidents/{id}", INCIDENT, "RANGER"),
            Arguments.of("GET", "/incidents", "", "PARK_MANAGER,RANGER"),
            Arguments.of("GET", "/incidents/{id}", "", "PARK_MANAGER,RANGER"),
            Arguments.of("PUT", "/alerts/{id}", ALERT, "PARK_MANAGER"),
            Arguments.of("GET", "/alerts", "", "PARK_MANAGER,RANGER,LIAISON_OFFICER"),
            Arguments.of("GET", "/alerts/{id}", "", "PARK_MANAGER,RANGER,LIAISON_OFFICER"),
            Arguments.of("POST", "/alerts/{id}/accept", "", "RANGER,LIAISON_OFFICER"),
            Arguments.of("POST", "/alerts/{id}/decline", "{\"reason\":\"Unavailable\"}", "RANGER,LIAISON_OFFICER"),
            Arguments.of("POST", "/alerts/{id}/support", "{\"requestId\":\"" + PHOTO + "\",\"reason\":\"Need help\"}", "RANGER,LIAISON_OFFICER"),
            Arguments.of("PUT", "/alerts/{id}/response", "{\"actionTaken\":\"Guided away\",\"result\":\"Resolved\"}", "RANGER,LIAISON_OFFICER"),
            Arguments.of("GET", "/analytics/summary?parkId=park&from=2026-10-01&to=2026-10-07", "", "PARK_MANAGER"),
            Arguments.of("PUT", "/reports/{id}", REPORT, "PARK_MANAGER"),
            Arguments.of("GET", "/reports?parkId=park", "", "PARK_MANAGER"),
            Arguments.of("GET", "/reports/{id}", "", "PARK_MANAGER"),
            Arguments.of("GET", "/reports/{id}/download", "", "PARK_MANAGER"),
            Arguments.of("PUT", "/community-reports/{id}", COMMUNITY, "COMMUNITY_MEMBER"),
            Arguments.of("GET", "/community-reports", "", "COMMUNITY_MEMBER,PARK_MANAGER,LIAISON_OFFICER"),
            Arguments.of("GET", "/community-reports/{id}", "", "COMMUNITY_MEMBER,PARK_MANAGER,LIAISON_OFFICER"),
            Arguments.of("GET", "/camera-traps?parkId=park", "", "PARK_MANAGER,RESEARCHER"),
            Arguments.of("PUT", "/camera-trap-images/{id}", CAMERA, "PARK_MANAGER,RESEARCHER"),
            Arguments.of("GET", "/camera-trap-images?parkId=park", "", "PARK_MANAGER,RESEARCHER"),
            Arguments.of("GET", "/camera-trap-images/{id}", "", "PARK_MANAGER,RESEARCHER"),
            Arguments.of("PUT", "/camera-trap-images/{id}/review", "{\"species\":\"Unknown\",\"possiblePoacher\":false}", "PARK_MANAGER,RESEARCHER")
        );
    }
}
