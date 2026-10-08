package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.domain.GeoPoint;
import com.wildlife.wildlife_conservationbackend.domain.PatrolObservation;
import com.wildlife.wildlife_conservationbackend.domain.PatrolRequest;
import com.wildlife.wildlife_conservationbackend.domain.TrackPoint;
import com.wildlife.wildlife_conservationbackend.domain.Waypoint;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.ParkArea;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolAssignmentEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolRouteEntity;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.AssignmentStatus;
import com.wildlife.wildlife_conservationbackend.enums.LocationSource;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoPersistenceTests {
    private static final Instant START = Instant.parse("2026-10-07T01:00:00Z");
    private MongoMappingContext context;
    private MappingMongoConverter converter;

    @BeforeEach
    void mongoMapping() throws Exception {
        context = new MongoMappingContext();
        converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.afterPropertiesSet();
        context.setSimpleTypeHolder(converter.getCustomConversions().getSimpleTypeHolder());
        context.setInitialEntitySet(Set.of(ParkEntity.class, PatrolRouteEntity.class,
                PatrolAssignmentEntity.class, PatrolEntity.class, UserEntity.class));
        context.afterPropertiesSet();
    }

    @Test
    void entitiesRoundTripThroughRealMongoConverter() {
        var location = new Location(6.37, 81.5, LocationSource.GPS, 8.0);
        var request = new PatrolRequest("assignment", START, START.plusSeconds(60),
                List.of(new TrackPoint(6.37, 81.5, START, 8.0)),
                List.of(new Waypoint("waypoint", "Waterhole", START, location, null)),
                List.of(new PatrolObservation("observation", "Elephants", START, location)));
        var patrol = new PatrolEntity("patrol", "assignment", "route", "park", "ranger", request.getEndedAt(),
                request, 0, START, "fingerprint");
        Document stored = new Document();
        converter.write(patrol, stored);
        assertThat(stored.get("_id")).isEqualTo("patrol");
        assertThat(converter.read(PatrolEntity.class, stored)).isEqualTo(patrol);
        var user = new UserEntity("ranger", "Ranger", "ranger@example.com", "hash", Role.RANGER, Set.of("park"), true);
        Document storedUser = new Document();
        converter.write(user, storedUser);
        assertThat(converter.read(UserEntity.class, storedUser)).isEqualTo(user);
        assertThat(user.toString()).doesNotContain("passwordHash", user.getPasswordHash());
        var park = new ParkEntity("park", "Park", "Asia/Colombo", List.of(new ParkArea("area", "Area")));
        Document storedPark = new Document();
        converter.write(park, storedPark);
        assertThat(storedPark.get("_id")).isEqualTo("park");
        assertThat(converter.read(ParkEntity.class, storedPark)).isEqualTo(park);
        var route = new PatrolRouteEntity("route", "park", "area", "Route", 125.5,
                List.of(new GeoPoint(6.37, 81.5), new GeoPoint(6.38, 81.5)));
        Document storedRoute = new Document();
        converter.write(route, storedRoute);
        assertThat(storedRoute.get("_id")).isEqualTo("route");
        assertThat(converter.read(PatrolRouteEntity.class, storedRoute)).isEqualTo(route);
    }

    @Test
    void requiredUniquenessIndexesAreDeclared() {
        var resolver = new MongoPersistentEntityIndexResolver(context);
        var assignmentIndexes = StreamSupport.stream(resolver.resolveIndexFor(PatrolAssignmentEntity.class).spliterator(), false).toList();
        assertThat(assignmentIndexes).anySatisfy(index -> {
            assertThat(index.getIndexKeys()).containsEntry("rangerId", 1).containsEntry("scheduledStartAt", 1);
            assertThat(index.getIndexOptions()).containsEntry("unique", true);
        });
        var patrolIndexes = StreamSupport.stream(resolver.resolveIndexFor(PatrolEntity.class).spliterator(), false).toList();
        assertThat(patrolIndexes).anySatisfy(index -> {
            assertThat(index.getIndexKeys()).containsEntry("assignmentId", 1);
            assertThat(index.getIndexOptions()).containsEntry("unique", true);
        });
        var userIndexes = StreamSupport.stream(resolver.resolveIndexFor(UserEntity.class).spliterator(), false).toList();
        assertThat(userIndexes).anySatisfy(index -> {
            assertThat(index.getIndexKeys()).containsEntry("normalizedEmail", 1);
            assertThat(index.getIndexOptions()).containsEntry("unique", true);
        });
    }

    @Test
    void completedStatusFilterPrecedesAssignmentPagination() {
        MongoTemplate template = mock(MongoTemplate.class);
        when(template.getConverter()).thenReturn(converter);
        var assignment = new PatrolAssignmentEntity("assignment", "park", "route", "ranger", START,
                START.plusSeconds(60), "manager", START, "fingerprint");
        Document row = new Document();
        converter.write(assignment, row);
        row.append("completedPatrols", List.of(new Document("_id", "patrol")));
        when(template.aggregate(any(Aggregation.class), eq("patrol_assignments"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(new Document("total", 1L)), new Document()))
                .thenReturn(new AggregationResults<>(List.of(row), new Document()));
        var page = new PatrolAssignmentQueryRepository(template).find(Criteria.where("parkId").is("park"),
                AssignmentStatus.COMPLETED, PageRequest.of(1, 20, Sort.by(Sort.Direction.DESC, "scheduledStartAt", "_id")));
        assertThat(page.getContent().get(0).isCompleted()).isTrue();
        assertThat(page.getContent().get(0).getAssignment()).isEqualTo(assignment);
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(template, times(2)).aggregate(captor.capture(), eq("patrol_assignments"), eq(Document.class));
        var pipeline = captor.getAllValues().get(1).toPipeline(Aggregation.DEFAULT_CONTEXT);
        assertThat(pipeline.get(2).get("$match", Document.class).get("completedPatrols.0", Document.class))
                .containsEntry("$exists", true);
        assertThat(pipeline.get(4)).containsEntry("$skip", 20L);
        assertThat(pipeline.get(1).get("$lookup", Document.class).getList("pipeline", Document.class))
                .containsExactly(new Document("$project", new Document("_id", 1)));
    }

    @Test
    void patrolSummaryProjectionDoesNotReturnGpsArrays() {
        MongoTemplate template = mock(MongoTemplate.class);
        Document row = new Document("_id", "patrol").append("id", "patrol")
                .append("assignmentId", "assignment").append("routeId", "route").append("parkId", "park")
                .append("rangerId", "ranger").append("status", "COMPLETED")
                .append("startedAt", java.util.Date.from(START)).append("endedAt", java.util.Date.from(START.plusSeconds(60)))
                .append("recordedDistanceMeters", 125.5).append("waypointCount", 2).append("observationCount", 1)
                .append("createdAt", java.util.Date.from(START.plusSeconds(120)));
        PatrolSummaryResponseDTO summary = converter.read(PatrolSummaryResponseDTO.class, row);
        when(template.count(any(Query.class), eq(PatrolEntity.class))).thenReturn(1L);
        when(template.aggregate(any(Aggregation.class), eq("patrols"), eq(PatrolSummaryResponseDTO.class)))
                .thenReturn(new AggregationResults<>(List.of(summary), new Document()));
        var page = new PatrolQueryRepository(template).find(Criteria.where("parkId").is("park"),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "endedAt", "_id")));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo("patrol");
        assertThat(summary.getStartedAt()).isEqualTo(START);
        assertThat(summary.getRecordedDistanceMeters()).isEqualTo(125.5);
        assertThat(summary.getWaypointCount()).isEqualTo(2);
        assertThat(summary.getObservationCount()).isEqualTo(1);
        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(template).aggregate(captor.capture(), eq("patrols"), eq(PatrolSummaryResponseDTO.class));
        var pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        var projection = pipeline.get(pipeline.size() - 1).get("$project", Document.class);
        assertThat(projection).doesNotContainKeys("submission", "trackPoints", "waypoints", "observations");
        assertThat(projection).containsKeys("waypointCount", "observationCount", "startedAt");
    }
}
