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
public class AlertSupportEntity {
    private String requestId;

    private String alertId;

    private String requestedBy;

    private String reason;

    private String status;

    private Instant requestedAt;
}
