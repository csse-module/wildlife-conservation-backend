package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.config.MediaStorageProperties;
import com.wildlife.wildlife_conservationbackend.config.ReportProperties;
import com.wildlife.wildlife_conservationbackend.domain.AlertResponseRequest;
import com.wildlife.wildlife_conservationbackend.domain.AnalyticsFacts;
import com.wildlife.wildlife_conservationbackend.domain.AnalyticsSnapshot;
import com.wildlife.wildlife_conservationbackend.domain.CameraImageRequest;
import com.wildlife.wildlife_conservationbackend.domain.CameraReviewRequest;
import com.wildlife.wildlife_conservationbackend.domain.CommunityReportRequest;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.DateRange;
import com.wildlife.wildlife_conservationbackend.domain.DeclineRequest;
import com.wildlife.wildlife_conservationbackend.domain.IncidentRequest;
import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.domain.MediaUploadRequest;
import com.wildlife.wildlife_conservationbackend.domain.ReportRequest;
import com.wildlife.wildlife_conservationbackend.domain.RouteCoverage;
import com.wildlife.wildlife_conservationbackend.domain.SupportRequest;
import com.wildlife.wildlife_conservationbackend.entity.AlertDeclineEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertResponseEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertSupportEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraImageEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraReviewEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraTrapEntity;
import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import com.wildlife.wildlife_conservationbackend.entity.IncidentEntity;
import com.wildlife.wildlife_conservationbackend.entity.MediaEntity;
import com.wildlife.wildlife_conservationbackend.entity.ParkArea;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolAssignmentEntity;
import com.wildlife.wildlife_conservationbackend.entity.ReportEntity;
import com.wildlife.wildlife_conservationbackend.enums.AlertStatus;
import com.wildlife.wildlife_conservationbackend.enums.CameraImageStatus;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportType;
import com.wildlife.wildlife_conservationbackend.enums.IncidentType;
import com.wildlife.wildlife_conservationbackend.enums.LocationSource;
import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import com.wildlife.wildlife_conservationbackend.enums.ReportSection;
import com.wildlife.wildlife_conservationbackend.enums.ReportType;
import com.wildlife.wildlife_conservationbackend.enums.RiskLevel;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.AlertMapper;
import com.wildlife.wildlife_conservationbackend.mapper.AnalyticsMapper;
import com.wildlife.wildlife_conservationbackend.mapper.CameraTrapMapper;
import com.wildlife.wildlife_conservationbackend.mapper.CommunityReportMapper;
import com.wildlife.wildlife_conservationbackend.mapper.IncidentMapper;
import com.wildlife.wildlife_conservationbackend.mapper.LocationMapper;
import com.wildlife.wildlife_conservationbackend.mapper.MediaMapper;
import com.wildlife.wildlife_conservationbackend.mapper.ReportMapper;
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
import com.wildlife.wildlife_conservationbackend.repository.PatrolAssignmentRepository;
import com.wildlife.wildlife_conservationbackend.repository.ReportRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.AlertServiceImpl;
import com.wildlife.wildlife_conservationbackend.service.impl.AnalyticsServiceImpl;
import com.wildlife.wildlife_conservationbackend.service.impl.CameraTrapServiceImpl;
import com.wildlife.wildlife_conservationbackend.service.impl.CommunityReportServiceImpl;
import com.wildlife.wildlife_conservationbackend.service.impl.IncidentServiceImpl;
import com.wildlife.wildlife_conservationbackend.service.impl.MediaServiceImpl;
import com.wildlife.wildlife_conservationbackend.service.impl.ReportServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.DateRangeResolver;
import com.wildlife.wildlife_conservationbackend.utility.ImageValidator;
import com.wildlife.wildlife_conservationbackend.utility.LocalMediaStorage;
import com.wildlife.wildlife_conservationbackend.utility.ObservationValidator;
import com.wildlife.wildlife_conservationbackend.utility.ReportPdfRenderer;
import com.wildlife.wildlife_conservationbackend.utility.RequestFingerprint;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureWorkflowTests {
    private static final Instant NOW = Instant.parse("2026-10-08T01:00:00Z");
    private static final CurrentUser RANGER = new CurrentUser("ranger", Role.RANGER, Set.of("park"));
    private static final CurrentUser MANAGER = new CurrentUser("manager", Role.PARK_MANAGER, Set.of("park"));
    private static final CurrentUser MEMBER = new CurrentUser("member", Role.COMMUNITY_MEMBER, Set.of("park"));
    private static final CurrentUser RESEARCHER = new CurrentUser("researcher", Role.RESEARCHER, Set.of("park"));
    @Mock private MediaRepository mediaRepository;
    @Mock private MediaAccessRepository mediaAccess;
    @Mock private MediaService ownedMedia;
    @Mock private ParkService parks;
    @Mock private ParkRepository parkRepository;
    @Mock private IncidentRepository incidents;
    @Mock private CommunityReportRepository communityReports;
    @Mock private PatrolAssignmentRepository assignments;
    @Mock private AlertRepository alerts;
    @Mock private AlertTransitionRepository alertTransitions;
    @Mock private CameraImageRepository images;
    @Mock private CameraTrapRepository cameras;
    @Mock private CameraImageTransitionRepository imageTransitions;
    @Mock private ReportRepository reports;
    @Mock private AnalyticsRepository analyticsRepository;
    @Mock private AnalyticsService analytics;
    @Mock private MongoPageReader pageReader;
    @TempDir private Path uploadDirectory;
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ResponseGenerator responses = new ResponseGenerator();
    private RequestFingerprint fingerprint;
    private MediaServiceImpl mediaService;
    private IncidentServiceImpl incidentService;
    private CommunityReportServiceImpl communityService;
    private AlertServiceImpl alertService;
    private CameraTrapServiceImpl cameraService;
    private ReportServiceImpl reportService;

    @BeforeEach
    void services() {
        fingerprint = new RequestFingerprint(JsonMapper.builder().findAndAddModules().build());
        var mediaSettings = new MediaStorageProperties();
        mediaSettings.setDirectory(uploadDirectory.toString());
        mediaService = new MediaServiceImpl(mediaRepository, new MediaMapper(), parks, mediaAccess,
                new LocalMediaStorage(mediaSettings), new ImageValidator(), responses, clock);
        var dates = new DateRangeResolver(parkRepository);
        var observations = new ObservationValidator(clock);
        var locationMapper = new LocationMapper();
        incidentService = new IncidentServiceImpl(incidents, new IncidentMapper(locationMapper), parks, ownedMedia,
                pageReader, dates, observations, fingerprint, responses, clock, assignments);
        communityService = new CommunityReportServiceImpl(communityReports, new CommunityReportMapper(locationMapper), parks,
                ownedMedia, pageReader, dates, observations, fingerprint, responses, clock);
        alertService = new AlertServiceImpl(alerts, alertTransitions, new AlertMapper(locationMapper), parks, ownedMedia,
                pageReader, observations, fingerprint, responses, clock);
        cameraService = new CameraTrapServiceImpl(images, cameras, imageTransitions, new CameraTrapMapper(), parks,
                ownedMedia, pageReader, observations, fingerprint, responses, clock);
        reportService = new ReportServiceImpl(reports, new ReportMapper(new AnalyticsMapper()), analytics, parks, pageReader,
                fingerprint, responses, new ReportPdfRenderer(new ReportProperties()));
    }

    @Test
    void imageUploadPersistsBytesAndIdenticalRetryDoesNotInsertAgain() throws Exception {
        AtomicReference<MediaEntity> saved = new AtomicReference<>();
        when(mediaRepository.findById("image")).thenAnswer(call -> Optional.ofNullable(saved.get()));
        when(mediaRepository.insert(any(MediaEntity.class))).thenAnswer(call -> {
            saved.set(call.getArgument(0));
            return saved.get();
        });
        byte[] bytes = png();
        var file = new MockMultipartFile("file", "photo.png", "image/png", bytes);
        var request = new MediaUploadRequest("park", MediaCategory.INCIDENT);
        var created = mediaService.upload(RANGER, "image", request, file);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).hasToString("/api/v1/media/image/content");
        assertThat(mediaService.upload(RANGER, "image", request, file).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mediaService.content(RANGER, "image").getBody()).isEqualTo(bytes);
        assertThat(created.getBody().getData().toString()).doesNotContain("storageKey", "sha256");
        verify(mediaRepository).insert(any(MediaEntity.class));
    }

    @Test
    void imageValidationRejectsFakeMimeAndOversizeOrWrongRole() throws Exception {
        var request = new MediaUploadRequest("park", MediaCategory.INCIDENT);
        var fake = new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> mediaService.upload(RANGER, "image", request, fake))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        var large = new MockMultipartFile("file", "large.png", "image/png", new byte[(int) ImageValidator.MAX_BYTES + 1]);
        assertThatThrownBy(() -> mediaService.upload(RANGER, "image", request, large))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        var file = new MockMultipartFile("file", "photo.png", "image/png", png());
        assertThatThrownBy(() -> mediaService.upload(MEMBER, "image", request, file))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        verify(mediaRepository, never()).insert(any(MediaEntity.class));
    }

    @Test
    void unlinkedMediaCannotBeReadByAnotherAccount() {
        when(mediaRepository.findById("image")).thenReturn(Optional.of(MediaEntity.builder().id("image")
                .ownerId("other").parkId("park").category(MediaCategory.INCIDENT).build()));
        assertThatThrownBy(() -> mediaService.content(RANGER, "image"))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void changedMediaMetadataConflictsWithoutWriting() throws Exception {
        byte[] bytes = png();
        String checksum = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        when(mediaRepository.findById("image")).thenReturn(Optional.of(MediaEntity.builder().id("image").ownerId("ranger")
                .parkId("park").category(MediaCategory.INCIDENT).sha256(checksum).build()));
        var file = new MockMultipartFile("file", "photo.png", "image/png", bytes);
        assertThatThrownBy(() -> mediaService.upload(RANGER, "image", new MediaUploadRequest("park", MediaCategory.ALERT_RESPONSE), file))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(mediaRepository, never()).insert(any(MediaEntity.class));
    }

    @Test
    void incidentRequiresMatchingAssignmentAndOwnedEvidence() {
        var request = incidentRequest();
        request.setAssignmentId("assignment");
        when(assignments.findById("assignment")).thenReturn(Optional.of(new PatrolAssignmentEntity("assignment", "park",
                "route", "different-ranger", NOW.minusSeconds(60), NOW, "manager", NOW, "hash")));
        assertThatThrownBy(() -> incidentService.submit(RANGER, "incident", request)).isInstanceOf(ApiException.class);
        verify(incidents, never()).insert(any(IncidentEntity.class));
        request.setAssignmentId(null);
        when(incidents.insert(any(IncidentEntity.class))).thenAnswer(call -> call.getArgument(0));
        assertThat(incidentService.submit(RANGER, "incident", request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(ownedMedia).requireOwnedMedia(RANGER, "image", "park", MediaCategory.INCIDENT);
    }

    @Test
    void concurrentIncidentRetryReturnsWinnerAndChangedReplayConflicts() {
        var request = incidentRequest();
        var existing = IncidentEntity.builder().id("incident").parkId("park").areaId("area").type(request.getType())
                .detectedAt(request.getDetectedAt()).location(request.getLocation()).description(request.getDescription())
                .photoId("image").reportedBy("ranger").createdAt(NOW).requestHash(fingerprint.of(request)).build();
        when(incidents.findById("incident")).thenReturn(Optional.empty()).thenReturn(Optional.of(existing));
        when(incidents.insert(any(IncidentEntity.class))).thenThrow(new DuplicateKeyException("race"));
        assertThat(incidentService.submit(RANGER, "incident", request).getStatusCode()).isEqualTo(HttpStatus.OK);
        request.setDescription("changed");
        assertThatThrownBy(() -> incidentService.submit(RANGER, "incident", request))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void communityReportAllowsUnknownCoordinatesButRequiresTypeDetails() {
        var request = new CommunityReportRequest("park", "area", CommunityReportType.CROP_DAMAGE, null,
                "Village", NOW, null, "Crop damage", null, null);
        assertThatThrownBy(() -> communityService.submit(MEMBER, "community", request)).isInstanceOf(ApiException.class);
        request.setCropDetails("Paddy damaged");
        when(communityReports.insert(any(CommunityReportEntity.class))).thenAnswer(call -> call.getArgument(0));
        var result = communityService.submit(MEMBER, "community", request);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getData().getLocation()).isNull();
        assertThat(result.getBody().getData().getReportedBy()).isEqualTo("member");
    }

    @Test
    void rangerIncidentQueriesAndCommunityQueriesApplyOwnerBeforePagination() {
        when(parks.accessibleParkIds(RANGER, null)).thenReturn(Set.of("park"));
        when(parks.accessibleParkIds(MEMBER, null)).thenReturn(Set.of("park"));
        when(pageReader.find(any(Criteria.class), any(PageRequest.class), eq(IncidentEntity.class))).thenReturn(new PageImpl<>(List.of()));
        when(pageReader.find(any(Criteria.class), any(PageRequest.class), eq(CommunityReportEntity.class))).thenReturn(new PageImpl<>(List.of()));
        incidentService.list(RANGER, new com.wildlife.wildlife_conservationbackend.dto.request.IncidentQuery(),
                new com.wildlife.wildlife_conservationbackend.dto.request.PageQuery());
        communityService.list(MEMBER, new com.wildlife.wildlife_conservationbackend.dto.request.CommunityReportQuery(),
                new com.wildlife.wildlife_conservationbackend.dto.request.PageQuery());
        var scope = ArgumentCaptor.forClass(Criteria.class);
        verify(pageReader).find(scope.capture(), any(PageRequest.class), eq(IncidentEntity.class));
        assertThat(scope.getValue().getCriteriaObject()).containsEntry("reportedBy", "ranger");
        verify(pageReader).find(scope.capture(), any(PageRequest.class), eq(CommunityReportEntity.class));
        assertThat(scope.getValue().getCriteriaObject()).containsEntry("reportedBy", "member");
    }

    @Test
    void concurrentAcceptLosesToAnotherOfficerButSameOfficerRetrySucceeds() {
        var fresh = alert(AlertStatus.NEW, null);
        when(alerts.findById("alert")).thenReturn(Optional.of(fresh), Optional.of(alert(AlertStatus.RESPONDING, "other")));
        assertThatThrownBy(() -> alertService.accept(RANGER, "alert"))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ALERT_ALREADY_ASSIGNED");
        when(alerts.findById("alert")).thenReturn(Optional.of(alert(AlertStatus.RESPONDING, "ranger")));
        assertThat(alertService.accept(RANGER, "alert").getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(alertTransitions).accept("alert", "park", "ranger", NOW);
    }

    @Test
    void declinedOfficerCannotAcceptAndChangedDeclineCannotOverwrite() {
        var fresh = alert(AlertStatus.NEW, null);
        fresh.setDeclines(List.of(new AlertDeclineEntity("alert", "ranger", "NEW", "Unavailable", NOW)));
        when(alerts.findById("alert")).thenReturn(Optional.of(fresh));
        assertThatThrownBy(() -> alertService.accept(RANGER, "alert")).isInstanceOf(ApiException.class);
        assertThat(alertService.decline(RANGER, "alert", new DeclineRequest("Unavailable")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatThrownBy(() -> alertService.decline(RANGER, "alert", new DeclineRequest("Changed")))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(alertTransitions, never()).accept(any(), any(), any(), any());
    }

    @Test
    void supportRetriesDoNotAppendAndRequestsAreBounded() {
        var responding = alert(AlertStatus.RESPONDING, "ranger");
        responding.setSupportRequests(List.of(new AlertSupportEntity("request", "alert", "ranger", "Need help", "REQUESTED", NOW)));
        when(alerts.findById("alert")).thenReturn(Optional.of(responding));
        assertThat(alertService.support(RANGER, "alert", new SupportRequest("request", "Need help")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThatThrownBy(() -> alertService.support(RANGER, "alert", new SupportRequest("request", "Changed"))).isInstanceOf(ApiException.class);
        responding.setSupportRequests(java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> new AlertSupportEntity("request-" + i, "alert", "ranger", "Need help", "REQUESTED", NOW)).toList());
        assertThatThrownBy(() -> alertService.support(RANGER, "alert", new SupportRequest("next", "Need help")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SUPPORT_LIMIT_REACHED");
        verify(alertTransitions, never()).support(any(), any(), any());
    }

    @Test
    void onlyAssignedOfficerResolvesAndResolutionRetryIsImmutable() {
        var request = new AlertResponseRequest("Guided animal away", "Resolved", null, List.of("image"));
        when(alerts.findById("alert")).thenReturn(Optional.of(alert(AlertStatus.RESPONDING, "other")));
        assertThatThrownBy(() -> alertService.resolve(RANGER, "alert", request)).isInstanceOf(ApiException.class);
        var responding = alert(AlertStatus.RESPONDING, "ranger");
        var resolved = alert(AlertStatus.RESOLVED, "ranger");
        resolved.setResponseHash(fingerprint.of(request));
        resolved.setResponse(new AlertResponseEntity(request.getActionTaken(), request.getResult(), null, request.getPhotoIds(), "ranger", NOW));
        when(alerts.findById("alert")).thenReturn(Optional.of(responding)).thenReturn(Optional.of(resolved));
        when(alertTransitions.resolve(eq("alert"), eq("park"), any(AlertResponseEntity.class), any())).thenReturn(resolved);
        assertThat(alertService.resolve(RANGER, "alert", request).getBody().getData().getStatus()).isEqualTo(AlertStatus.RESOLVED);
        verify(ownedMedia).requireOwnedMedia(RANGER, "image", "park", MediaCategory.ALERT_RESPONSE);
        assertThat(alertService.resolve(RANGER, "alert", request).getStatusCode()).isEqualTo(HttpStatus.OK);
        request.setResult("Changed");
        assertThatThrownBy(() -> alertService.resolve(RANGER, "alert", request)).isInstanceOf(ApiException.class);
        verify(alertTransitions).resolve(eq("alert"), eq("park"), any(AlertResponseEntity.class), any());
    }

    @Test
    void cameraSubmissionValidatesOwnedMediaAndFinalReviewCannotBeReplaced() {
        when(cameras.findById("camera")).thenReturn(Optional.of(new CameraTrapEntity("camera", "park", "area", "Camera", location())));
        when(images.insert(any(CameraImageEntity.class))).thenAnswer(call -> call.getArgument(0));
        assertThat(cameraService.submit(RESEARCHER, "image-record", new CameraImageRequest("camera", "image", NOW)).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        verify(ownedMedia).requireOwnedMedia(RESEARCHER, "image", "park", MediaCategory.CAMERA_TRAP);
        var request = new CameraReviewRequest("Elephant", false, null);
        var reviewed = CameraImageEntity.builder().id("image-record").parkId("park").status(CameraImageStatus.REVIEWED)
                .review(new CameraReviewEntity("Elephant", false, null, "researcher", NOW)).reviewHash(fingerprint.of(request)).build();
        when(images.findById("image-record")).thenReturn(Optional.of(reviewed));
        assertThat(cameraService.review(RESEARCHER, "image-record", request).getStatusCode()).isEqualTo(HttpStatus.OK);
        request.setPossiblePoacher(true);
        assertThatThrownBy(() -> cameraService.review(RESEARCHER, "image-record", request)).isInstanceOf(ApiException.class);
        verify(imageTransitions, never()).review(any(), any(), any(), any());
    }

    @Test
    void analyticsReportsNullCoverageWhenThereAreNoAssignments() {
        var service = new AnalyticsServiceImpl(parks, parkRepository, new DateRangeResolver(parkRepository),
                analyticsRepository, new AnalyticsMapper(), responses, clock);
        when(parkRepository.findById("park")).thenReturn(Optional.of(new ParkEntity("park", "Park", "Asia/Colombo",
                List.of(new ParkArea("area", "Area")))));
        when(analyticsRepository.summarize(eq("park"), any(DateRange.class)))
                .thenReturn(new AnalyticsFacts(Map.of(), Map.of(), Map.of(), 0, 0, 0, 0, 0));
        var result = service.snapshot(MANAGER, "park", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));
        assertThat(result.isDataAvailable()).isFalse();
        assertThat(result.getPatrolCoverage().getCoveragePercent()).isNull();
        assertThat(result.getDailyIncidentCounts()).hasSize(7);
    }

    @Test
    void reportsRetryAndDownloadUseSavedSnapshotWithoutRecalculation() throws Exception {
        var request = new ReportRequest("park", ReportType.MONTHLY_CONSERVATION, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 7), List.of(ReportSection.PATROL_COVERAGE));
        var snapshot = new AnalyticsSnapshot("park", request.getFrom(), request.getTo(), true, 0, List.of(), List.of(), List.of(),
                new RouteCoverage("ASSIGNED_ROUTES_COMPLETED", 2, 1, 50.0), 0, 0, NOW);
        when(analytics.snapshot(MANAGER, "park", request.getFrom(), request.getTo())).thenReturn(snapshot);
        AtomicReference<ReportEntity> saved = new AtomicReference<>();
        when(reports.findById("report")).thenAnswer(call -> Optional.ofNullable(saved.get()));
        when(reports.insert(any(ReportEntity.class))).thenAnswer(call -> {
            saved.set(call.getArgument(0));
            return saved.get();
        });
        assertThat(reportService.generate(MANAGER, "report", request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reportService.generate(MANAGER, "report", request).getStatusCode()).isEqualTo(HttpStatus.OK);
        byte[] pdf = reportService.download(MANAGER, "report").getBody();
        try (var document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(document)).contains("Route completion coverage: 1 / 2", "50.0%");
        }
        verify(analytics).snapshot(MANAGER, "park", request.getFrom(), request.getTo());
        verify(reports).insert(any(ReportEntity.class));
    }

    private byte[] png() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }

    private Location location() {
        return new Location(6.37, 81.5, LocationSource.GPS, 8.0);
    }

    private IncidentRequest incidentRequest() {
        return new IncidentRequest("park", "area", null, IncidentType.SNARE, NOW, location(), "Snare found", "image");
    }

    private AlertEntity alert(AlertStatus status, String officer) {
        return AlertEntity.builder().id("alert").parkId("park").areaId("area").animal("Elephant").collarId("collar")
                .riskLevel(RiskLevel.HIGH).location(location()).locationUpdatedAt(NOW).detectedAt(NOW)
                .status(status).createdBy("manager").createdAt(NOW).assignedOfficerId(officer).acceptedAt(NOW)
                .declines(List.of()).supportRequests(List.of()).build();
    }
}
