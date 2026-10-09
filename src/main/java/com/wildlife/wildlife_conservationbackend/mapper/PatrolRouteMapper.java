package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.dto.response.PatrolRouteResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.PatrolRouteEntity;
import org.springframework.stereotype.Component;

@Component
public class PatrolRouteMapper {
    public PatrolRouteResponseDTO toResponse(PatrolRouteEntity route) {
        return new PatrolRouteResponseDTO(route.getId(), route.getParkId(), route.getAreaId(), route.getName(),
                route.getPlannedDistanceMeters(), route.getPathPoints());
    }
}
