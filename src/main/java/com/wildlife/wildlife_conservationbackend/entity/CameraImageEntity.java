package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.enums.CameraImageStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("camera_trap_images")
@CompoundIndex(name = "park_capturedAt", def = "{'parkId': 1, 'capturedAt': -1, '_id': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraImageEntity {
    @MongoId
    private String id;

    private String cameraTrapId;

    @Indexed
    private String mediaId;

    private Instant capturedAt;

    @Indexed
    private String parkId;

    private CameraImageStatus status;

    private String uploadedBy;

    private Instant createdAt;

    private String requestHash;

    private CameraReviewEntity review;

    private String reviewHash;
}
