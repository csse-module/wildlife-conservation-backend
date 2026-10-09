package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<UserEntity, String> {
    Optional<UserEntity> findByNormalizedEmail(String normalizedEmail);
}
