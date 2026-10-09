package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.enums.LocationSource;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequestDTO {
    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    private Double latitude;

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    private Double longitude;

    @NotNull
    private LocationSource source;

    @DecimalMin("0")
    @DecimalMax("10000")
    private Double accuracyMeters;
}
