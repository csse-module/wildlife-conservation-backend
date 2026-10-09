package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponseDTO {
    private String id;

    private String parkId;

    private MediaCategory category;

    private String contentType;

    private long sizeBytes;

    private String contentUrl;

    private Instant createdAt;
}
