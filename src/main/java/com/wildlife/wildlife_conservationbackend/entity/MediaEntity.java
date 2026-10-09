package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaEntity {
    @MongoId
    private String id;

    private String ownerId;

    @Indexed
    private String parkId;

    private MediaCategory category;

    private String contentType;

    private long sizeBytes;

    private String sha256;

    private String storageKey;

    private Instant createdAt;
}
