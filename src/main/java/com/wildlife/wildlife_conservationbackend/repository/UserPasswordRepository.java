package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPasswordRepository {
    private final MongoTemplate mongoTemplate;

    public UserEntity changePassword(UserEntity expected, String newHash) {
        Criteria version = expected.getTokenVersion() == 0
                ? new Criteria().orOperator(Criteria.where("tokenVersion").is(0), Criteria.where("tokenVersion").exists(false))
                : Criteria.where("tokenVersion").is(expected.getTokenVersion());
        Criteria condition = Criteria.where("_id").is(expected.getId()).and("active").is(true)
                .and("passwordHash").is(expected.getPasswordHash()).andOperator(version);
        Update update = new Update().set("passwordHash", newHash).set("passwordChangeRequired", false).inc("tokenVersion", 1);
        return mongoTemplate.findAndModify(Query.query(condition), update,
                FindAndModifyOptions.options().returnNew(true), UserEntity.class);
    }
}
