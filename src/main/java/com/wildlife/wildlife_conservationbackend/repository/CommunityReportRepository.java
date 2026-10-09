package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommunityReportRepository extends MongoRepository<CommunityReportEntity, String> { }
