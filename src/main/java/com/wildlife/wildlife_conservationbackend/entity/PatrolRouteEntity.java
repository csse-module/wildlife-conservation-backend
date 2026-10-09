package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.domain.GeoPoint;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("patrol_routes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolRouteEntity {
    @MongoId
    private String id;
    @Indexed
    private String parkId;
    private String areaId;
    private String name;
    private double plannedDistanceMeters;
    private List<GeoPoint> pathPoints;
}
