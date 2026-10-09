package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.enums.CameraImageStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraImageQuery {
    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = ".*\\S.*")
    private String parkId;

    @Size(max = 80)
    @Pattern(regexp = ".*\\S.*")
    private String cameraTrapId;

    private CameraImageStatus status;
}
