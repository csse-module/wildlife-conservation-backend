package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.AlertResponseRequest;
import com.wildlife.wildlife_conservationbackend.domain.AlertSetupRequest;
import com.wildlife.wildlife_conservationbackend.domain.DeclineRequest;
import com.wildlife.wildlife_conservationbackend.domain.SupportRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.AlertResponseRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.AlertSetupRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.DeclineRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.SupportRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertDeclineResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertResolutionResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertSupportResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.AlertDeclineEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertResponseEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertSupportEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertMapper {
    private final LocationMapper locationMapper;

    public AlertSetupRequest toRequest(AlertSetupRequestDTO dto) {
        return new AlertSetupRequest(dto.getParkId(), dto.getAreaId(), dto.getAnimal(), dto.getCollarId(), dto.getRiskLevel(),
                locationMapper.toLocation(dto.getLocation()), dto.getLocationUpdatedAt(), dto.getDetectedAt());
    }

    public AlertResponseRequest toRequest(AlertResponseRequestDTO dto) {
        return new AlertResponseRequest(dto.getActionTaken(), dto.getResult(), dto.getNotes(),
                dto.getPhotoIds() == null ? List.of() : dto.getPhotoIds());
    }

    public DeclineRequest toRequest(DeclineRequestDTO dto) {
        return new DeclineRequest(dto.getReason());
    }

    public SupportRequest toRequest(SupportRequestDTO dto) {
        return new SupportRequest(dto.getRequestId(), dto.getReason());
    }

    public AlertResponseDTO toResponse(AlertEntity entity) {
        return new AlertResponseDTO(entity.getId(), entity.getParkId(), entity.getAreaId(), entity.getAnimal(),
                entity.getCollarId(), entity.getRiskLevel(), entity.getLocation(), entity.getLocationUpdatedAt(),
                entity.getDetectedAt(), entity.getStatus(), entity.getCreatedAt(), entity.getAssignedOfficerId(),
                entity.getAcceptedAt(), entity.getSupportRequests().stream().map(this::toResponse).toList(),
                toResponse(entity.getResponse()));
    }

    public AlertDeclineResponseDTO toResponse(AlertDeclineEntity entity) {
        return new AlertDeclineResponseDTO(entity.getAlertId(), entity.getOfficerId(), entity.getStatus(),
                entity.getReason(), entity.getRecordedAt());
    }

    public AlertSupportResponseDTO toResponse(AlertSupportEntity entity) {
        return new AlertSupportResponseDTO(entity.getRequestId(), entity.getAlertId(), entity.getRequestedBy(),
                entity.getReason(), entity.getStatus(), entity.getRequestedAt());
    }

    private AlertResolutionResponseDTO toResponse(AlertResponseEntity entity) {
        return entity == null ? null : new AlertResolutionResponseDTO(entity.getActionTaken(), entity.getResult(),
                entity.getNotes(), entity.getPhotoIds(), entity.getRecordedBy(), entity.getRecordedAt());
    }
}
