package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.IncidentRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.IncidentRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.IncidentResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.IncidentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncidentMapper {
    private final LocationMapper locationMapper;

    public IncidentRequest toRequest(IncidentRequestDTO dto) {
        return new IncidentRequest(dto.getParkId(), dto.getAreaId(), dto.getAssignmentId(), dto.getType(), dto.getDetectedAt(),
                locationMapper.toLocation(dto.getLocation()), dto.getDescription(), dto.getPhotoId());
    }

    public IncidentResponseDTO toResponse(IncidentEntity entity) {
        return new IncidentResponseDTO(entity.getId(), entity.getParkId(), entity.getAreaId(), entity.getAssignmentId(),
                entity.getType(), entity.getDetectedAt(), entity.getLocation(), entity.getDescription(), entity.getPhotoId(),
                entity.getReportedBy(), "SUBMITTED", entity.getCreatedAt());
    }
}
