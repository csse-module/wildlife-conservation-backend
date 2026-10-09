package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.PatrolMapper;
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
public class PatrolController {
    private final PatrolService patrolService;
    private final PatrolMapper mapper;

    @PutMapping(value = EndPoint.PATROLS + "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('RANGER')")
    public ResponseEntity<StandardResponse<PatrolResponseDTO>> complete(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody PatrolRequestDTO request) {
        return patrolService.complete(actor, id, mapper.toRequest(request));
    }

    @GetMapping(EndPoint.PATROLS)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER')")
    public ResponseEntity<StandardResponse<PageResponse<PatrolSummaryResponseDTO>>> list(
            @AuthenticationPrincipal CurrentUser actor, @Valid @ModelAttribute PatrolQuery query,
            @Valid @ModelAttribute PageQuery page) {
        return patrolService.listPatrols(actor, query, page);
    }

    @GetMapping(EndPoint.PATROLS + "/{id}")
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER')")
    public ResponseEntity<StandardResponse<PatrolResponseDTO>> get(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id) {
        return patrolService.getPatrol(actor, id);
    }
}
