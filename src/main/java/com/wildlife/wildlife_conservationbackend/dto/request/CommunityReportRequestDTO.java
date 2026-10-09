package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.enums.CommunityReportType;
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
public class CommunityReportRequestDTO {
    @NotBlank
    @Size(max = 80)
    private String parkId;

    @NotBlank
    @Size(max = 80)
    private String areaId;

    @NotNull
    private CommunityReportType type;

    @Size(max = 100)
    @Pattern(regexp = ".*\\S.*")
    private String species;

    @NotBlank
    @Size(max = 150)
    private String village;

    @NotNull
    private Instant occurredAt;

    @Valid
    private LocationRequestDTO location;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    @Pattern(regexp = ".*\\S.*")
    private String cropDetails;

    @Pattern(regexp = ValidationPatterns.UUID)
    private String photoId;
}
