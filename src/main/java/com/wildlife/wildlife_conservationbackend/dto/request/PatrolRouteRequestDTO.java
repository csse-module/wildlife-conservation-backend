package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.domain.GeoPoint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class PatrolRouteRequestDTO {
    @Size(max = 100)
    private String parkId;

    @Size(max = 100)
    private String areaId;

    @NotBlank
    @Size(max = 200)
    private String name;

    private double plannedDistanceMeters;

    @NotNull
    private List<GeoPoint> pathPoints;
}
