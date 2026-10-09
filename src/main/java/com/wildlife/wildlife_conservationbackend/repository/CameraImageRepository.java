package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.CameraImageEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CameraImageRepository extends MongoRepository<CameraImageEntity, String> { }
