package com.wildlife.wildlife_conservationbackend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MongoPageReader {
    private final MongoTemplate mongoTemplate;

    public <T> Page<T> find(Criteria criteria, PageRequest page, Class<T> type) {
        long total = mongoTemplate.count(Query.query(criteria), type);
        return new PageImpl<>(mongoTemplate.find(Query.query(criteria).with(page), type), page, total);
    }
}
