package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.domain.CommunityResponseRequest;
import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportStatus;
import java.time.Instant;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CommunityTransitionTests {
    @Test
    void claimsAndResolutionsMatchParkOwnerAndStateAtomicallyIncludingLegacyReports() {
        var mongo = mock(MongoTemplate.class);
        var repository = new CommunityReportTransitionRepository(mongo);
        Instant now = Instant.parse("2026-10-09T01:00:00Z");
        repository.accept("report", "park", "officer", now);
        repository.resolve("report", "park", "officer", new CommunityResponseRequest("Visited village", "Resolved"), now);
        var queries = ArgumentCaptor.forClass(Query.class);
        var updates = ArgumentCaptor.forClass(Update.class);
        verify(mongo, times(2)).findAndModify(queries.capture(), updates.capture(), any(FindAndModifyOptions.class), eq(CommunityReportEntity.class));
        Document claim = queries.getAllValues().get(0).getQueryObject();
        assertThat(claim).containsEntry("_id", "report").containsEntry("parkId", "park").containsEntry("assignedOfficerId", null);
        assertThat(claim.toString()).contains("SUBMITTED", "$or", "null");
        assertThat(queries.getAllValues().get(1).getQueryObject()).containsEntry("status", CommunityReportStatus.RESPONDING)
                .containsEntry("assignedOfficerId", "officer").containsEntry("parkId", "park");
        assertThat(updates.getAllValues().get(1).getUpdateObject().get("$set", Document.class))
                .containsEntry("status", CommunityReportStatus.RESOLVED).containsEntry("resolvedBy", "officer")
                .containsEntry("resolvedAt", now).containsEntry("actionTaken", "Visited village");
    }
}
