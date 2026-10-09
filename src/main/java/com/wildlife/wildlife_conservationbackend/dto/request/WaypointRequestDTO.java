package com.wildlife.wildlife_conservationbackend.dto.request;

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
public class WaypointRequestDTO {
    @NotBlank
    @Pattern(regexp = ValidationPatterns.UUID)
    private String id;

    @NotBlank
    @Size(max = 100)
    private String label;

    @NotNull
    private Instant recordedAt;

    @NotNull
    @Valid
    private LocationRequestDTO location;

    @Size(max = 1000)
    private String notes;
}
