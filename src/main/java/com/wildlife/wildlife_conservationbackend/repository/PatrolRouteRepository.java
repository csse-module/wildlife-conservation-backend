package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.PatrolRouteEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatrolRouteRepository extends MongoRepository<PatrolRouteEntity, String> {
}
