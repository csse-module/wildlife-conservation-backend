package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.utility.ValidationPatterns;
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
public class CameraImageRequestDTO {
    @NotBlank
    @Size(max = 80)
    private String cameraTrapId;

    @NotBlank
    @Pattern(regexp = ValidationPatterns.UUID)
    private String mediaId;

    @NotNull
    private Instant capturedAt;
}
