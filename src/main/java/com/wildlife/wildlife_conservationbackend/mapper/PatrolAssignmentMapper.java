package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.PatrolAssignmentRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolAssignmentRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolAssignmentResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.PatrolAssignmentEntity;
import com.wildlife.wildlife_conservationbackend.enums.AssignmentStatus;
import org.springframework.stereotype.Component;

@Component
public class PatrolAssignmentMapper {
    public PatrolAssignmentRequest toRequest(PatrolAssignmentRequestDTO dto) {
        return new PatrolAssignmentRequest(dto.getRouteId(), dto.getRangerId(), dto.getScheduledStartAt(), dto.getScheduledEndAt());
    }

    public PatrolAssignmentResponseDTO toResponse(PatrolAssignmentEntity entity, boolean completed) {
        return new PatrolAssignmentResponseDTO(entity.getId(), entity.getParkId(), entity.getRouteId(), entity.getRangerId(),
                entity.getScheduledStartAt(), entity.getScheduledEndAt(),
                completed ? AssignmentStatus.COMPLETED : AssignmentStatus.ASSIGNED, entity.getAssignedBy(), entity.getCreatedAt());
    }
}
