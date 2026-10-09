package com.wildlife.wildlife_conservationbackend.dto.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertDeclineResponseDTO {
    private String alertId;

    private String officerId;

    private String status;

    private String reason;

    private Instant recordedAt;
}
