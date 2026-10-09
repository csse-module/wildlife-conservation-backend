package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.dto.response.ParkResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import org.springframework.stereotype.Component;

import com.wildlife.wildlife_conservationbackend.dto.request.ParkCreateRequestDTO;
import com.wildlife.wildlife_conservationbackend.entity.ParkArea;
import java.util.stream.Collectors;

@Component
public class ParkMapper {
    public ParkResponseDTO toResponse(ParkEntity park) {
        return new ParkResponseDTO(park.getId(), park.getName(), park.getTimezone(), park.getAreas());
    }

    public ParkEntity toEntity(ParkCreateRequestDTO request) {
        ParkEntity entity = new ParkEntity();
        entity.setId(request.getId());
        entity.setName(request.getName());
        entity.setTimezone(request.getTimezone());
        entity.setAreas(request.getAreas().stream()
                .map(a -> new ParkArea(a.getId(), a.getName()))
                .collect(Collectors.toList()));
        return entity;
    }
}
