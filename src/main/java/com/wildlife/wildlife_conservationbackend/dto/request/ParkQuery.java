package com.wildlife.wildlife_conservationbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkQuery {
    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = ".*\\S.*")
    private String parkId;
}
