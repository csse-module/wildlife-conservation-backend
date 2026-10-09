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
public class PatrolObservationRequestDTO {
    @NotBlank
    @Pattern(regexp = ValidationPatterns.UUID)
    private String id;

    @NotBlank
    @Size(max = 2000)
    private String text;

    @NotNull
    private Instant observedAt;

    @NotNull
    @Valid
    private LocationRequestDTO location;
}
