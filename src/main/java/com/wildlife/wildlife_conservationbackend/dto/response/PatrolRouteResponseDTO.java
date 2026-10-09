package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.domain.GeoPoint;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolRouteResponseDTO {
    private String id;
    private String parkId;
    private String areaId;
    private String name;
    private double plannedDistanceMeters;
    private List<GeoPoint> pathPoints;
}
