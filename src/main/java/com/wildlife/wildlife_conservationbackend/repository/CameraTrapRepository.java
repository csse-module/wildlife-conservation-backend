package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.CameraTrapEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CameraTrapRepository extends MongoRepository<CameraTrapEntity, String> { }
