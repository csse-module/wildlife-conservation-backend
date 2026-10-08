package com.wildlife.wildlife_conservationbackend.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDeclineEntity {
    private String alertId;

    private String officerId;

    private String status;

    private String reason;

    private Instant recordedAt;
}
