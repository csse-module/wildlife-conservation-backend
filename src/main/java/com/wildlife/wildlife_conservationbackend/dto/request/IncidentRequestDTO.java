package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.enums.IncidentType;
import com.wildlife.wildlife_conservationbackend.utility.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRequestDTO {
    @NotBlank
    @Size(max = 80)
    private String parkId;

    @NotBlank
    @Size(max = 80)
    private String areaId;

    @Pattern(regexp = ValidationPatterns.UUID)
    private String assignmentId;

    @NotNull
    private IncidentType type;

    @NotNull
    private Instant detectedAt;

    @NotNull
    @Valid
    private LocationRequestDTO location;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @NotBlank
    @Pattern(regexp = ValidationPatterns.UUID)
    private String photoId;
}
