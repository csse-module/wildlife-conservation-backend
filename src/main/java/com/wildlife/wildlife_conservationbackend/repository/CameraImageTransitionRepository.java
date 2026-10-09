package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.CameraImageEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraReviewEntity;
import com.wildlife.wildlife_conservationbackend.enums.CameraImageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CameraImageTransitionRepository {
    private final MongoTemplate mongoTemplate;

    public CameraImageEntity review(String id, String parkId, CameraReviewEntity review, String hash) {
        Criteria condition = Criteria.where("_id").is(id).and("parkId").is(parkId)
                .and("status").is(CameraImageStatus.PENDING_REVIEW).and("review").is(null);
        Update update = new Update().set("status", CameraImageStatus.REVIEWED).set("review", review).set("reviewHash", hash);
        return mongoTemplate.findAndModify(Query.query(condition), update,
                FindAndModifyOptions.options().returnNew(true), CameraImageEntity.class);
    }
}
