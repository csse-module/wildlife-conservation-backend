package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.AlertEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertDeclineEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertSupportEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertResponseEntity;
import com.wildlife.wildlife_conservationbackend.enums.AlertStatus;
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
public class AlertTransitionRepository {
    private final MongoTemplate mongoTemplate;

    public AlertEntity accept(String id, String parkId, String officerId, Instant now) {
        Criteria condition = base(id, parkId).and("status").is(AlertStatus.NEW)
                .and("assignedOfficerId").is(null).and("declines.officerId").ne(officerId);
        Update update = new Update().set("status", AlertStatus.RESPONDING)
                .set("assignedOfficerId", officerId).set("acceptedAt", now);
        return update(condition, update);
    }

    public AlertEntity decline(String id, String parkId, AlertDeclineEntity decline) {
        Criteria condition = base(id, parkId).and("status").is(AlertStatus.NEW)
                .and("assignedOfficerId").is(null).and("declines.officerId").ne(decline.getOfficerId())
                .and("declines.99").exists(false);
        return update(condition, new Update().push("declines", decline));
    }

    public AlertEntity support(String id, String parkId, AlertSupportEntity support) {
        Criteria condition = base(id, parkId).and("status").is(AlertStatus.RESPONDING)
                .and("assignedOfficerId").is(support.getRequestedBy())
                .and("supportRequests.requestId").ne(support.getRequestId()).and("supportRequests.19").exists(false);
        return update(condition, new Update().push("supportRequests", support));
    }

    public AlertEntity resolve(String id, String parkId, AlertResponseEntity response, String hash) {
        Criteria condition = base(id, parkId).and("status").is(AlertStatus.RESPONDING)
                .and("assignedOfficerId").is(response.getRecordedBy()).and("response").is(null);
        Update update = new Update().set("response", response).set("responseHash", hash)
                .set("status", AlertStatus.RESOLVED).set("resolvedAt", response.getRecordedAt());
        return update(condition, update);
    }

    private Criteria base(String id, String parkId) {
        return Criteria.where("_id").is(id).and("parkId").is(parkId);
    }

    private AlertEntity update(Criteria condition, Update update) {
        return mongoTemplate.findAndModify(Query.query(condition), update,
                FindAndModifyOptions.options().returnNew(true), AlertEntity.class);
    }
}
