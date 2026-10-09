package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.MediaUploadRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.MediaResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    ResponseEntity<StandardResponse<MediaResponseDTO>> upload(CurrentUser actor, String id, MediaUploadRequest request, MultipartFile file);
    ResponseEntity<byte[]> content(CurrentUser actor, String id);
    void requireOwnedMedia(CurrentUser actor, String id, String parkId, MediaCategory category);
}
