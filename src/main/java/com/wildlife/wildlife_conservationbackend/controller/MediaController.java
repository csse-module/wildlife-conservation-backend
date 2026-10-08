package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.MediaUploadRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.MediaResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.MediaMapper;
import com.wildlife.wildlife_conservationbackend.service.MediaService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import com.wildlife.wildlife_conservationbackend.utility.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EndPoint.BASE)
@RequiredArgsConstructor
public class MediaController {
    private final MediaService service;
    private final MediaMapper mapper;

    @PutMapping(value = EndPoint.MEDIA + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER', 'LIAISON_OFFICER', 'RESEARCHER', 'COMMUNITY_MEMBER')")
    public ResponseEntity<StandardResponse<MediaResponseDTO>> upload(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @ModelAttribute MediaUploadRequestDTO request) {
        return service.upload(actor, id, mapper.toRequest(request), request.getFile());
    }

    @GetMapping(value = EndPoint.MEDIA + "/{id}/content")
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER', 'LIAISON_OFFICER', 'RESEARCHER', 'COMMUNITY_MEMBER')")
    public ResponseEntity<byte[]> content(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id) {
        return service.content(actor, id);
    }
}
