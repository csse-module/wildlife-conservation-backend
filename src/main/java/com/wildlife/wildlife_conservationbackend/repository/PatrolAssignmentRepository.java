package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.PatrolAssignmentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatrolAssignmentRepository extends MongoRepository<PatrolAssignmentEntity, String> {
}
