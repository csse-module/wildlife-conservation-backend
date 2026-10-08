package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.utility.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseRequestDTO {
    @NotBlank
    @Size(max = 1000)
    private String actionTaken;

    @NotBlank
    @Size(max = 500)
    private String result;

    @Size(max = 2000)
    @Pattern(regexp = ".*\\S.*")
    private String notes;

    @Size(max = 3)
    private List<@NotNull @Pattern(regexp = ValidationPatterns.UUID) String> photoIds = List.of();
}
