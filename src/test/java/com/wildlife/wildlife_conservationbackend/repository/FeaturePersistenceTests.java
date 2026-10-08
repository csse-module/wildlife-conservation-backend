package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.domain.AnalyticsSnapshot;
import com.wildlife.wildlife_conservationbackend.domain.AreaCount;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.DailyCount;
import com.wildlife.wildlife_conservationbackend.domain.DateRange;
import com.wildlife.wildlife_conservationbackend.domain.IncidentTypeCount;
import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.domain.RouteCoverage;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeaturePersistenceTests {
    private static final Instant NOW = Instant.parse("2026-10-08T01:00:00Z");

    @Test
    void allNewDocumentsAndEmbeddedSnapshotsRoundTripWithRealMongoConverter() throws Exception {
        var context = new MongoMappingContext();
        var converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.afterPropertiesSet();
        context.setSimpleTypeHolder(converter.getCustomConversions().getSimpleTypeHolder());
        context.setInitialEntitySet(Set.of(MediaEntity.class, IncidentEntity.class, CommunityReportEntity.class,
                AlertEntity.class, CameraTrapEntity.class, CameraImageEntity.class, ReportEntity.class));
        context.afterPropertiesSet();
        var location = new Location(6.37, 81.5, LocationSource.GPS, 8.0);
        roundTrip(converter, MediaEntity.class, MediaEntity.builder().id("media").ownerId("ranger").parkId("park")
                .category(MediaCategory.INCIDENT).contentType("image/png").sizeBytes(50).sha256("hash").storageKey("key").createdAt(NOW).build());
        roundTrip(converter, IncidentEntity.class, IncidentEntity.builder().id("incident").parkId("park").areaId("area")
                .type(IncidentType.SNARE).detectedAt(NOW).location(location).description("Snare").photoId("media")
                .reportedBy("ranger").createdAt(NOW).requestHash("hash").build());
        roundTrip(converter, CommunityReportEntity.class, CommunityReportEntity.builder().id("community").parkId("park")
                .areaId("area").type(CommunityReportType.CROP_DAMAGE).village("Village").occurredAt(NOW)
                .description("Paddy damaged").cropDetails("Paddy").reportedBy("member").createdAt(NOW).requestHash("hash").build());
        roundTrip(converter, CameraTrapEntity.class, new CameraTrapEntity("camera", "park", "area", "Camera", location));
        roundTrip(converter, CameraImageEntity.class, CameraImageEntity.builder().id("image").parkId("park")
                .cameraTrapId("camera").mediaId("media").capturedAt(NOW).status(CameraImageStatus.REVIEWED)
                .uploadedBy("researcher").createdAt(NOW).requestHash("hash")
                .review(new CameraReviewEntity("Unknown", true, "Verify", "researcher", NOW)).reviewHash("review-hash").build());
        roundTrip(converter, AlertEntity.class, AlertEntity.builder().id("alert").parkId("park").areaId("area")
                .animal("Elephant").collarId("collar").riskLevel(RiskLevel.HIGH).location(location).locationUpdatedAt(NOW)
                .detectedAt(NOW).status(AlertStatus.RESOLVED).createdBy("manager").createdAt(NOW).requestHash("hash")
                .assignedOfficerId("ranger").acceptedAt(NOW).resolvedAt(NOW)
                .declines(List.of(new AlertDeclineEntity("alert", "other", "NEW", "Unavailable", NOW)))
                .supportRequests(List.of(new AlertSupportEntity("request", "alert", "ranger", "Need help", "REQUESTED", NOW)))
                .response(new AlertResponseEntity("Guided away", "Resolved", null, List.of("media"), "ranger", NOW))
                .responseHash("response-hash").build());
        LocalDate date = LocalDate.of(2026, 10, 7);
        var snapshot = new AnalyticsSnapshot("park", date, date, true, 3,
                List.of(new IncidentTypeCount(IncidentType.SNARE, 3)), List.of(new DailyCount(date, 3)),
                List.of(new AreaCount("area", "Area", 3, true)), new RouteCoverage("ASSIGNED_ROUTES_COMPLETED", 0, 0, null), 0, 1, NOW);
        roundTrip(converter, ReportEntity.class, ReportEntity.builder().id("report").parkId("park")
                .reportType(ReportType.MONTHLY_CONSERVATION).from(date).to(date).sections(List.of(ReportSection.HOTSPOT_SUMMARY))
                .generatedBy("manager").generatedAt(NOW).snapshot(snapshot).requestHash("hash").build());
    }

    @Test
    void alertUpdatesMatchStateOwnerAndDeduplicationBoundsInOneMongoOperation() {
        MongoTemplate template = mock(MongoTemplate.class);
        var repository = new AlertTransitionRepository(template);
        repository.accept("alert", "park", "ranger", NOW);
        repository.decline("alert", "park", new AlertDeclineEntity("alert", "ranger", "NEW", "Unavailable", NOW));
        repository.support("alert", "park", new AlertSupportEntity("request", "alert", "ranger", "Need help", "REQUESTED", NOW));
        repository.resolve("alert", "park", new AlertResponseEntity("Guided away", "Resolved", null, List.of(), "ranger", NOW), "hash");
        var queries = ArgumentCaptor.forClass(Query.class);
        var updates = ArgumentCaptor.forClass(Update.class);
        verify(template, times(4)).findAndModify(queries.capture(), updates.capture(), any(FindAndModifyOptions.class), eq(AlertEntity.class));
        assertThat(queries.getAllValues().get(0).getQueryObject()).containsEntry("status", AlertStatus.NEW)
                .containsEntry("assignedOfficerId", null).containsEntry("parkId", "park");
        assertThat(queries.getAllValues().get(0).getQueryObject().get("declines.officerId", Document.class)).containsEntry("$ne", "ranger");
        assertThat(queries.getAllValues().get(1).getQueryObject().get("declines.99", Document.class)).containsEntry("$exists", false);
        assertThat(queries.getAllValues().get(2).getQueryObject()).containsEntry("assignedOfficerId", "ranger");
        assertThat(queries.getAllValues().get(2).getQueryObject().get("supportRequests.19", Document.class)).containsEntry("$exists", false);
        assertThat(queries.getAllValues().get(2).getQueryObject().get("supportRequests.requestId", Document.class)).containsEntry("$ne", "request");
        assertThat(queries.getAllValues().get(3).getQueryObject()).containsEntry("status", AlertStatus.RESPONDING).containsEntry("response", null);
        assertThat(updates.getAllValues().get(3).getUpdateObject().get("$set", Document.class))
                .containsEntry("status", AlertStatus.RESOLVED).containsEntry("resolvedAt", NOW).containsEntry("responseHash", "hash");
    }

    @Test
    void finalCameraReviewRequiresPendingStateAndCannotOverwriteExistingReview() {
        MongoTemplate template = mock(MongoTemplate.class);
        var review = new CameraReviewEntity("Unknown", true, null, "researcher", NOW);
        new CameraImageTransitionRepository(template).review("image", "park", review, "hash");
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        verify(template).findAndModify(query.capture(), update.capture(), any(FindAndModifyOptions.class), eq(CameraImageEntity.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("status", CameraImageStatus.PENDING_REVIEW)
                .containsEntry("parkId", "park").containsEntry("review", null);
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class))
                .containsEntry("status", CameraImageStatus.REVIEWED).containsEntry("review", review).containsEntry("reviewHash", "hash");
    }

    @Test
    void mediaReadPolicyUsesParentOwnerAndParkScope() {
        MongoTemplate template = mock(MongoTemplate.class);
        var ranger = new CurrentUser("ranger", Role.RANGER, Set.of("park"));
        when(template.exists(any(Query.class), eq(IncidentEntity.class))).thenReturn(true);
        assertThat(new MediaAccessRepository(template).isLinkedAndReadable(ranger, "photo")).isTrue();
        var query = ArgumentCaptor.forClass(Query.class);
        verify(template).exists(query.capture(), eq(IncidentEntity.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("reportedBy", "ranger").containsEntry("photoId", "photo");
        assertThat(query.getValue().getQueryObject().get("parkId", Document.class).get("$in")).isEqualTo(Set.of("park"));
    }

    @Test
    void coverageOnlyCountsScheduledRoutesWithLinkedPatrolsEndingWithinThePeriod() {
        MongoTemplate template = mock(MongoTemplate.class);
        when(template.aggregate(any(Aggregation.class), any(Class.class), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(), new Document()));
        var range = new DateRange(NOW.minusSeconds(86400), NOW, "Asia/Colombo");
        new AnalyticsRepository(template).summarize("park", range);
        var captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(template).aggregate(captor.capture(), eq(PatrolAssignmentEntity.class), eq(Document.class));
        List<Document> pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        assertThat(pipeline.get(0).get("$match", Document.class)).containsKeys("parkId", "scheduledStartAt");
        assertThat(pipeline.get(1).get("$lookup", Document.class)).containsEntry("from", "patrols")
                .containsEntry("let", new Document("assignmentId", "$_id"));
        assertThat(pipeline.get(1).toJson()).contains("$assignmentId", "$endedAt", "$gte", "$lt");
        assertThat(pipeline.get(2).get("$group", Document.class)).containsEntry("_id", "$routeId").containsKey("completed");
    }

    private <T> void roundTrip(MappingMongoConverter converter, Class<T> type, T entity) {
        Document document = new Document();
        converter.write(entity, document);
        assertThat(document).containsKey("_id");
        assertThat(converter.read(type, document)).isEqualTo(entity);
    }
}
