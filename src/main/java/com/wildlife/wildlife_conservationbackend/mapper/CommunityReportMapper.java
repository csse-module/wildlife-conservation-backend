package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.CommunityReportRequest;
import com.wildlife.wildlife_conservationbackend.domain.CommunityResponseRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.CommunityResponseRequestDTO;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportStatus;
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
                entity.getCropDetails(), entity.getPhotoId(), entity.getReportedBy(),
                entity.getStatus() == null ? CommunityReportStatus.SUBMITTED.name() : entity.getStatus().name(), entity.getCreatedAt(),
                entity.getAssignedOfficerId(), entity.getAcceptedAt(), entity.getActionTaken(), entity.getResult(),
                entity.getResolvedBy(), entity.getResolvedAt());
    }

    public CommunityResponseRequest toResponseRequest(CommunityResponseRequestDTO dto) {
        return new CommunityResponseRequest(dto.getActionTaken().strip(), dto.getResult().strip());
    }
}
