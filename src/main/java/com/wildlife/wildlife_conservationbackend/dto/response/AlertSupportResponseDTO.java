package com.wildlife.wildlife_conservationbackend.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSupportResponseDTO {
    private String requestId;

    private String alertId;

    private String requestedBy;

    private String reason;

    private String status;

    private Instant requestedAt;
}
