package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.domain.CommunityResponseRequest;
import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportStatus;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommunityReportTransitionRepository {
    private final MongoTemplate mongoTemplate;

    public CommunityReportEntity accept(String id, String parkId, String officerId, Instant now) {
        Criteria condition = base(id, parkId).and("assignedOfficerId").is(null).orOperator(
                Criteria.where("status").is(CommunityReportStatus.SUBMITTED), Criteria.where("status").is(null));
        Update update = new Update().set("status", CommunityReportStatus.RESPONDING)
                .set("assignedOfficerId", officerId).set("acceptedAt", now);
        return update(condition, update);
    }

    public CommunityReportEntity resolve(String id, String parkId, String officerId,
                                        CommunityResponseRequest request, Instant now) {
        Criteria condition = base(id, parkId).and("assignedOfficerId").is(officerId)
                .and("status").is(CommunityReportStatus.RESPONDING);
        Update update = new Update().set("status", CommunityReportStatus.RESOLVED)
                .set("actionTaken", request.getActionTaken()).set("result", request.getResult())
                .set("resolvedBy", officerId).set("resolvedAt", now);
        return update(condition, update);
    }

    private Criteria base(String id, String parkId) {
        return Criteria.where("_id").is(id).and("parkId").is(parkId);
    }

    private CommunityReportEntity update(Criteria condition, Update update) {
        return mongoTemplate.findAndModify(Query.query(condition), update,
                FindAndModifyOptions.options().returnNew(true), CommunityReportEntity.class);
    }
}
