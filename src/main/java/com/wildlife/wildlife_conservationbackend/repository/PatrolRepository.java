package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.PatrolEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatrolRepository extends MongoRepository<PatrolEntity, String> {
    boolean existsByAssignmentId(String assignmentId);
}
