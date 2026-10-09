package com.wildlife.wildlife_conservationbackend.domain;

import com.wildlife.wildlife_conservationbackend.enums.LocationSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    private Double latitude;

    private Double longitude;

    private LocationSource source;

    private Double accuracyMeters;
}
