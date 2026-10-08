package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.CameraImageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.CameraImageRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.CameraReviewRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.ParkQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraImageResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraTrapResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.CameraTrapMapper;
import com.wildlife.wildlife_conservationbackend.service.CameraTrapService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EndPoint.BASE)
@RequiredArgsConstructor
public class CameraTrapController {
    private final CameraTrapService service;
    private final CameraTrapMapper mapper;

    @GetMapping(value = EndPoint.CAMERA_TRAPS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RESEARCHER')")
    public ResponseEntity<StandardResponse<PageResponse<CameraTrapResponseDTO>>> listTraps(
            @AuthenticationPrincipal CurrentUser actor,
            @Valid @ModelAttribute ParkQuery query,
            @Valid @ModelAttribute PageQuery page) {
        return service.listTraps(actor, query.getParkId(), page);
    }

    @PutMapping(value = EndPoint.CAMERA_IMAGES + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RESEARCHER')")
    public ResponseEntity<StandardResponse<CameraImageResponseDTO>> submit(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody CameraImageRequestDTO request) {
        return service.submit(actor, id, mapper.toRequest(request));
    }

    @GetMapping(value = EndPoint.CAMERA_IMAGES, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RESEARCHER')")
    public ResponseEntity<StandardResponse<PageResponse<CameraImageResponseDTO>>> listImages(
            @AuthenticationPrincipal CurrentUser actor,
            @Valid @ModelAttribute CameraImageQuery query,
            @Valid @ModelAttribute PageQuery page) {
        return service.listImages(actor, query, page);
    }

    @GetMapping(value = EndPoint.CAMERA_IMAGES + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RESEARCHER')")
    public ResponseEntity<StandardResponse<CameraImageResponseDTO>> get(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id) {
        return service.get(actor, id);
    }

    @PutMapping(value = EndPoint.CAMERA_IMAGES + "/{id}/review", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RESEARCHER')")
    public ResponseEntity<StandardResponse<CameraImageResponseDTO>> review(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody CameraReviewRequestDTO request) {
        return service.review(actor, id, mapper.toReviewRequest(request));
    }
}
