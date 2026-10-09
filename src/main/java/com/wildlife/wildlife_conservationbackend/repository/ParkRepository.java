package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ParkRepository extends MongoRepository<ParkEntity, String> {
}
