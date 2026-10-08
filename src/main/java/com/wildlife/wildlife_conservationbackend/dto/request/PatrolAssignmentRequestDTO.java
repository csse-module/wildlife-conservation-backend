package com.wildlife.wildlife_conservationbackend.dto.request;

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
public class PatrolAssignmentRequestDTO {
    @NotBlank
    @Size(max = 100)
    private String routeId;

    @NotBlank
    @Size(max = 100)
    private String rangerId;

    @NotNull
    private Instant scheduledStartAt;

    @NotNull
    private Instant scheduledEndAt;
}
