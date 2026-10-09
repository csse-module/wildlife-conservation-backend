package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.MediaEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MediaRepository extends MongoRepository<MediaEntity, String> { }
