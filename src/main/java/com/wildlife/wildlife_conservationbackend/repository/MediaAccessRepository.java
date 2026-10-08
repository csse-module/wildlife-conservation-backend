package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.entity.AlertEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraImageEntity;
import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import com.wildlife.wildlife_conservationbackend.entity.IncidentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MediaAccessRepository {
    private final MongoTemplate mongoTemplate;

    public boolean isLinkedAndReadable(CurrentUser actor, String mediaId) {
        return switch (actor.getRole()) {
            case PARK_MANAGER -> incident(actor, mediaId, false) || community(actor, mediaId, false)
                    || camera(actor, mediaId) || alert(actor, mediaId);
            case RANGER -> incident(actor, mediaId, true) || alert(actor, mediaId);
            case LIAISON_OFFICER -> community(actor, mediaId, false) || alert(actor, mediaId);
            case RESEARCHER -> camera(actor, mediaId);
            case COMMUNITY_MEMBER -> community(actor, mediaId, true);
        };
    }

    private boolean incident(CurrentUser actor, String id, boolean ownOnly) {
        Criteria scope = scope(actor).and("photoId").is(id);
        if (ownOnly) {
            scope.and("reportedBy").is(actor.getId());
        }
        return mongoTemplate.exists(Query.query(scope), IncidentEntity.class);
    }

    private boolean community(CurrentUser actor, String id, boolean ownOnly) {
        Criteria scope = scope(actor).and("photoId").is(id);
        if (ownOnly) {
            scope.and("reportedBy").is(actor.getId());
        }
        return mongoTemplate.exists(Query.query(scope), CommunityReportEntity.class);
    }

    private boolean camera(CurrentUser actor, String id) {
        return mongoTemplate.exists(Query.query(scope(actor).and("mediaId").is(id)), CameraImageEntity.class);
    }

    private boolean alert(CurrentUser actor, String id) {
        return mongoTemplate.exists(Query.query(scope(actor).and("response.photoIds").is(id)), AlertEntity.class);
    }

    private Criteria scope(CurrentUser actor) {
        return Criteria.where("parkId").in(actor.getParkIds());
    }
}
