package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.CommunityReportRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.CommunityReportRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.CommunityReportResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityReportMapper {
    private final LocationMapper locationMapper;

    public CommunityReportRequest toRequest(CommunityReportRequestDTO dto) {
        return new CommunityReportRequest(dto.getParkId(), dto.getAreaId(), dto.getType(), dto.getSpecies(), dto.getVillage(),
                dto.getOccurredAt(), locationMapper.toLocation(dto.getLocation()), dto.getDescription(), dto.getCropDetails(),
                dto.getPhotoId());
    }

    public CommunityReportResponseDTO toResponse(CommunityReportEntity entity) {
        return new CommunityReportResponseDTO(entity.getId(), entity.getParkId(), entity.getAreaId(), entity.getType(),
                entity.getSpecies(), entity.getVillage(), entity.getOccurredAt(), entity.getLocation(), entity.getDescription(),
                entity.getCropDetails(), entity.getPhotoId(), entity.getReportedBy(), "SUBMITTED", entity.getCreatedAt());
    }
}
