package com.wildlife.wildlife_conservationbackend.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResolutionResponseDTO {
    private String actionTaken;

    private String result;

    private String notes;

    private List<String> photoIds;

    private String recordedBy;

    private Instant recordedAt;
}
