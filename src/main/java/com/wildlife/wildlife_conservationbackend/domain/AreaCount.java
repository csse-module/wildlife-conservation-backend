package com.wildlife.wildlife_conservationbackend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaCount {
    private String areaId;

    private String areaName;

    private long incidentCount;

    private boolean potentialHotspot;
}
