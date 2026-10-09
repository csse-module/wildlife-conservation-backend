package com.wildlife.wildlife_conservationbackend.entity;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseEntity {
    private String actionTaken;

    private String result;

    private String notes;

    private List<String> photoIds;

    private String recordedBy;

    private Instant recordedAt;
}
