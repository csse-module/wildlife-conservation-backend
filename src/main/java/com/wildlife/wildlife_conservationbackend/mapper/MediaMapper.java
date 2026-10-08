package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.MediaUploadRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.MediaUploadRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.MediaResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.MediaEntity;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import org.springframework.stereotype.Component;

@Component

public class MediaMapper {

    public MediaUploadRequest toRequest(MediaUploadRequestDTO dto) {
        return new MediaUploadRequest(dto.getParkId(), dto.getCategory());
    }

    public MediaResponseDTO toResponse(MediaEntity entity) {
        return new MediaResponseDTO(entity.getId(), entity.getParkId(), entity.getCategory(), entity.getContentType(),
                entity.getSizeBytes(), EndPoint.BASE + EndPoint.MEDIA + "/" + entity.getId() + "/content", entity.getCreatedAt());
    }
}
