package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.IncidentQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.IncidentRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.IncidentResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.IncidentMapper;
import com.wildlife.wildlife_conservationbackend.service.IncidentService;
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
public class IncidentController {
    private final IncidentService service;
    private final IncidentMapper mapper;

    @PutMapping(value = EndPoint.INCIDENTS + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('RANGER')")
    public ResponseEntity<StandardResponse<IncidentResponseDTO>> submit(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody IncidentRequestDTO request) {
        return service.submit(actor, id, mapper.toRequest(request));
    }

    @GetMapping(value = EndPoint.INCIDENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER')")
    public ResponseEntity<StandardResponse<PageResponse<IncidentResponseDTO>>> list(
            @AuthenticationPrincipal CurrentUser actor,
            @Valid @ModelAttribute IncidentQuery query,
            @Valid @ModelAttribute PageQuery page) {
        return service.list(actor, query, page);
    }

    @GetMapping(value = EndPoint.INCIDENTS + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER')")
    public ResponseEntity<StandardResponse<IncidentResponseDTO>> get(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id) {
        return service.get(actor, id);
    }
}
