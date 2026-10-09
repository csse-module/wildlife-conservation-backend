package com.wildlife.wildlife_conservationbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraReviewRequestDTO {
    @NotBlank
    @Size(max = 100)
    private String species;

    @NotNull
    private Boolean possiblePoacher;

    @Size(max = 2000)
    @Pattern(regexp = ".*\\S.*")
    private String notes;
}
