package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.domain.AssignmentView;
import com.wildlife.wildlife_conservationbackend.entity.PatrolAssignmentEntity;
import com.wildlife.wildlife_conservationbackend.enums.AssignmentStatus;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PatrolAssignmentQueryRepository {
    private final MongoTemplate mongoTemplate;

    public Page<AssignmentView> find(Criteria scope, AssignmentStatus status, PageRequest page) {
        List<AggregationOperation> filtered = filtered(scope, status);
        List<AggregationOperation> countPipeline = new ArrayList<>(filtered);
        countPipeline.add(Aggregation.count().as("total"));
        Document count = mongoTemplate.aggregate(Aggregation.newAggregation(countPipeline),
                "patrol_assignments", Document.class).getUniqueMappedResult();
        long total = count == null ? 0 : ((Number) count.get("total")).longValue();
        List<AggregationOperation> pagePipeline = new ArrayList<>(filtered);
        pagePipeline.add(Aggregation.sort(page.getSort()));
        pagePipeline.add(Aggregation.skip(page.getOffset()));
        pagePipeline.add(Aggregation.limit(page.getPageSize()));
        List<AssignmentView> items = mongoTemplate.aggregate(Aggregation.newAggregation(pagePipeline),
                "patrol_assignments", Document.class).getMappedResults().stream().map(this::toView).toList();
        return new PageImpl<>(items, page, total);
    }

    private List<AggregationOperation> filtered(Criteria scope, AssignmentStatus status) {
        List<AggregationOperation> pipeline = new ArrayList<>();
        pipeline.add(Aggregation.match(scope));
        // Only project the matching patrol ID; GPS arrays never enter this lookup result.
        pipeline.add(context -> new Document("$lookup", new Document("from", "patrols")
                .append("localField", "_id").append("foreignField", "assignmentId")
                .append("pipeline", List.of(new Document("$project", new Document("_id", 1))))
                .append("as", "completedPatrols")));
        if (status != null) {
            pipeline.add(Aggregation.match(Criteria.where("completedPatrols.0")
                    .exists(status == AssignmentStatus.COMPLETED)));
        }
        return pipeline;
    }

    private AssignmentView toView(Document row) {
        return new AssignmentView(mongoTemplate.getConverter().read(PatrolAssignmentEntity.class, row),
                !row.getList("completedPatrols", Document.class).isEmpty());
    }
}
