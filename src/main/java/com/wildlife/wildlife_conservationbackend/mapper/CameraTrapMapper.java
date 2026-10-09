package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.CameraImageRequest;
import com.wildlife.wildlife_conservationbackend.domain.CameraReviewRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.CameraImageRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.CameraReviewRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraImageResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraReviewResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraTrapResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.CameraImageEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraReviewEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraTrapEntity;
import org.springframework.stereotype.Component;

@Component
public class CameraTrapMapper {
    public CameraImageRequest toRequest(CameraImageRequestDTO dto) {
        return new CameraImageRequest(dto.getCameraTrapId(), dto.getMediaId(), dto.getCapturedAt());
    }

    public CameraReviewRequest toReviewRequest(CameraReviewRequestDTO dto) {
        return new CameraReviewRequest(dto.getSpecies(), dto.getPossiblePoacher(), dto.getNotes());
    }

    public CameraTrapResponseDTO toResponse(CameraTrapEntity entity) {
        return new CameraTrapResponseDTO(entity.getId(), entity.getParkId(), entity.getAreaId(), entity.getName(),
                entity.getLocation());
    }

    public CameraImageResponseDTO toResponse(CameraImageEntity entity) {
        return new CameraImageResponseDTO(entity.getId(), entity.getCameraTrapId(), entity.getMediaId(), entity.getCapturedAt(),
                entity.getParkId(), entity.getStatus(), entity.getUploadedBy(), entity.getCreatedAt(),
                toReviewResponse(entity.getReview()));
    }

    private CameraReviewResponseDTO toReviewResponse(CameraReviewEntity review) {
        return review == null ? null : new CameraReviewResponseDTO(review.getSpecies(), review.getPossiblePoacher(),
                review.getNotes(), review.getReviewedBy(), review.getReviewedAt());
    }
}
