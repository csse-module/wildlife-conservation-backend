package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.AssignmentQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolAssignmentRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolAssignmentResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.PatrolAssignmentMapper;
import com.wildlife.wildlife_conservationbackend.service.PatrolService;
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
@RequestMapping(value = EndPoint.BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PatrolAssignmentController {
    private final PatrolService patrolService;
    private final PatrolAssignmentMapper mapper;

    @PutMapping(value = EndPoint.ASSIGNMENTS + "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PARK_MANAGER')")
    public ResponseEntity<StandardResponse<PatrolAssignmentResponseDTO>> assign(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody PatrolAssignmentRequestDTO request) {
        return patrolService.assign(actor, id, mapper.toRequest(request));
    }

    @GetMapping(EndPoint.ASSIGNMENTS)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER')")
    public ResponseEntity<StandardResponse<PageResponse<PatrolAssignmentResponseDTO>>> list(
            @AuthenticationPrincipal CurrentUser actor, @Valid @ModelAttribute AssignmentQuery query,
            @Valid @ModelAttribute PageQuery page) {
        return patrolService.listAssignments(actor, query, page);
    }
}
