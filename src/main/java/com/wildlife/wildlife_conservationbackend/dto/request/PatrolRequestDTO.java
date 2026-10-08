package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.utility.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolRequestDTO {
    @NotBlank
    @Pattern(regexp = ValidationPatterns.UUID)
    private String assignmentId;

    @NotNull
    private Instant startedAt;

    @NotNull
    private Instant endedAt;

    @NotNull
    @Size(max = 10000)
    private List<@NotNull @Valid TrackPointRequestDTO> trackPoints;

    @NotNull
    @Size(max = 100)
    private List<@NotNull @Valid WaypointRequestDTO> waypoints;

    @NotNull
    @Size(max = 100)
    private List<@NotNull @Valid PatrolObservationRequestDTO> observations;
}
