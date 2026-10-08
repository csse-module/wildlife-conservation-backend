package com.wildlife.wildlife_conservationbackend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteCoverage {
    private String basis;

    private long assignedRouteCount;

    private long completedRouteCount;

    private Double coveragePercent;
}
