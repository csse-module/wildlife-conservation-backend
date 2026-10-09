package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.ReportEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReportRepository extends MongoRepository<ReportEntity, String> { }
