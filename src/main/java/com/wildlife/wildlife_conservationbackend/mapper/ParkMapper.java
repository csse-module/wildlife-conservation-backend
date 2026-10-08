package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.dto.response.ParkResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import org.springframework.stereotype.Component;

@Component
public class ParkMapper {
    public ParkResponseDTO toResponse(ParkEntity park) {
        return new ParkResponseDTO(park.getId(), park.getName(), park.getTimezone(), park.getAreas());
    }
}
