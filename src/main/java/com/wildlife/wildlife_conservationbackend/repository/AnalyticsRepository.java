package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.domain.AnalyticsFacts;
import com.wildlife.wildlife_conservationbackend.domain.DateRange;
import com.wildlife.wildlife_conservationbackend.entity.AlertEntity;
import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import com.wildlife.wildlife_conservationbackend.entity.IncidentEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolAssignmentEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolEntity;
import com.wildlife.wildlife_conservationbackend.enums.AlertStatus;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AnalyticsRepository {
    private final MongoTemplate mongoTemplate;

    public AnalyticsFacts summarize(String parkId, DateRange range) {
        Criteria incidents = period(parkId, "detectedAt", range);
        Map<String, Long> types = counts(incidents, "type", IncidentEntity.class);
        Map<String, Long> areas = counts(incidents, "areaId", IncidentEntity.class);
        Map<String, Long> days = dailyCounts(incidents, range.getTimezone());
        long community = mongoTemplate.count(Query.query(period(parkId, "occurredAt", range)), CommunityReportEntity.class);
        long resolved = mongoTemplate.count(Query.query(period(parkId, "resolvedAt", range).and("status").is(AlertStatus.RESOLVED)), AlertEntity.class);
        long patrols = mongoTemplate.count(Query.query(period(parkId, "endedAt", range)), PatrolEntity.class);
        Document coverage = coverage(parkId, range);
        return new AnalyticsFacts(types, days, areas, community, resolved, patrols,
                number(coverage, "assignedRouteCount"), number(coverage, "completedRouteCount"));
    }

    private <T> Map<String, Long> counts(Criteria scope, String field, Class<T> entityClass) {
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(scope), Aggregation.group(field).count().as("count"));
        var rows = mongoTemplate.aggregate(aggregation, entityClass, Document.class).getMappedResults();
        return countMap(rows);
    }

    private Map<String, Long> dailyCounts(Criteria scope, String timezone) {
        Document date = new Document("$dateToString", new Document("format", "%Y-%m-%d")
                .append("date", "$detectedAt").append("timezone", timezone));
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(scope),
                context -> new Document("$group", new Document("_id", date).append("count", new Document("$sum", 1))));
        return countMap(mongoTemplate.aggregate(aggregation, IncidentEntity.class, Document.class).getMappedResults());
    }

    private Document coverage(String parkId, DateRange range) {
        var patrolCondition = new Document("$and", List.of(
                new Document("$eq", List.of("$assignmentId", "$$assignmentId")),
                new Document("$gte", List.of("$endedAt", Date.from(range.getStartInclusive()))),
                new Document("$lt", List.of("$endedAt", Date.from(range.getEndExclusive())))));
        Document lookup = new Document("$lookup", new Document("from", "patrols")
                .append("let", new Document("assignmentId", "$_id"))
                .append("pipeline", List.of(new Document("$match", new Document("$expr", patrolCondition)),
                        new Document("$limit", 1), new Document("$project", new Document("_id", 1))))
                .append("as", "completedPatrols"));
        Document completed = new Document("$cond", List.of(
                new Document("$gt", List.of(new Document("$size", "$completedPatrols"), 0)), 1, 0));
        Document byRoute = new Document("$group", new Document("_id", "$routeId")
                .append("completed", new Document("$max", completed)));
        Document totals = new Document("$group", new Document("_id", null)
                .append("assignedRouteCount", new Document("$sum", 1))
                .append("completedRouteCount", new Document("$sum", "$completed")));
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(period(parkId, "scheduledStartAt", range)),
                context -> lookup, context -> byRoute, context -> totals);
        var rows = mongoTemplate.aggregate(aggregation, PatrolAssignmentEntity.class, Document.class).getMappedResults();
        return rows.isEmpty() ? new Document() : rows.get(0);
    }

    private Criteria period(String parkId, String field, DateRange range) {
        return Criteria.where("parkId").is(parkId).and(field).gte(range.getStartInclusive()).lt(range.getEndExclusive());
    }

    private Map<String, Long> countMap(List<Document> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(row.getString("_id"), number(row, "count")));
        return result;
    }

    private long number(Document row, String field) {
        Number value = row.get(field, Number.class);
        return value == null ? 0 : value.longValue();
    }
}
