package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.dto.request.LocationRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class LocationMapper {
    public Location toLocation(LocationRequestDTO dto) {
        return dto == null ? null : new Location(dto.getLatitude(), dto.getLongitude(), dto.getSource(), dto.getAccuracyMeters());
    }
}
