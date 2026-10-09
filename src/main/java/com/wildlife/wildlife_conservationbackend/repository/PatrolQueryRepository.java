package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.dto.response.PatrolSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.PatrolEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PatrolQueryRepository {
    private final MongoTemplate mongoTemplate;

    public Page<PatrolSummaryResponseDTO> find(Criteria scope, PageRequest page) {
        long total = mongoTemplate.count(Query.query(scope), PatrolEntity.class);
        var projection = Aggregation.project("assignmentId", "routeId", "parkId", "rangerId", "endedAt",
                        "recordedDistanceMeters", "createdAt")
                .and("_id").as("id")
                .and("submission.startedAt").as("startedAt")
                .andExpression("'COMPLETED'").as("status")
                .and(ArrayOperators.Size.lengthOfArray("submission.waypoints")).as("waypointCount")
                .and(ArrayOperators.Size.lengthOfArray("submission.observations")).as("observationCount");
        var aggregation = Aggregation.newAggregation(Aggregation.match(scope), Aggregation.sort(page.getSort()),
                Aggregation.skip(page.getOffset()), Aggregation.limit(page.getPageSize()), projection);
        var items = mongoTemplate.aggregate(aggregation, "patrols", PatrolSummaryResponseDTO.class).getMappedResults();
        return new PageImpl<>(items, page, total);
    }
}
