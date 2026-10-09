package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.CameraImageRequest;
import com.wildlife.wildlife_conservationbackend.domain.CameraReviewRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.CameraImageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraImageResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraTrapResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface CameraTrapService {
    ResponseEntity<StandardResponse<PageResponse<CameraTrapResponseDTO>>> listTraps(CurrentUser actor, String parkId, PageQuery page);
    ResponseEntity<StandardResponse<CameraImageResponseDTO>> submit(CurrentUser actor, String id, CameraImageRequest request);
    ResponseEntity<StandardResponse<PageResponse<CameraImageResponseDTO>>> listImages(CurrentUser actor, CameraImageQuery query, PageQuery page);
    ResponseEntity<StandardResponse<CameraImageResponseDTO>> get(CurrentUser actor, String id);
    ResponseEntity<StandardResponse<CameraImageResponseDTO>> review(CurrentUser actor, String id, CameraReviewRequest request);
}
