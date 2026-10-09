package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.enums.RiskLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSetupRequestDTO {
    @NotBlank
    @Size(max = 80)
    private String parkId;

    @NotBlank
    @Size(max = 80)
    private String areaId;

    @NotBlank
    @Size(max = 100)
    private String animal;

    @NotBlank
    @Size(max = 80)
    private String collarId;

    @NotNull
    private RiskLevel riskLevel;

    @NotNull
    @Valid
    private LocationRequestDTO location;

    @NotNull
    private Instant locationUpdatedAt;

    @NotNull
    private Instant detectedAt;
}
