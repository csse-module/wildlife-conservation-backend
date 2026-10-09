package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.AlertQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.AlertResponseRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.AlertSetupRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.DeclineRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.SupportRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertDeclineResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertSupportResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.AlertMapper;
import com.wildlife.wildlife_conservationbackend.service.AlertService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EndPoint.BASE)
@RequiredArgsConstructor
public class AlertController {
    private final AlertService service;
    private final AlertMapper mapper;

    @PutMapping(value = EndPoint.ALERTS + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PARK_MANAGER')")
    public ResponseEntity<StandardResponse<AlertResponseDTO>> create(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody AlertSetupRequestDTO request) {
        return service.create(actor, id, mapper.toRequest(request));
    }

    @GetMapping(value = EndPoint.ALERTS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER', 'LIAISON_OFFICER')")
    public ResponseEntity<StandardResponse<PageResponse<AlertResponseDTO>>> list(
            @AuthenticationPrincipal CurrentUser actor,
            @Valid @ModelAttribute AlertQuery query,
            @Valid @ModelAttribute PageQuery page) {
        return service.list(actor, query, page);
    }

    @GetMapping(value = EndPoint.ALERTS + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER', 'LIAISON_OFFICER')")
    public ResponseEntity<StandardResponse<AlertResponseDTO>> get(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id) {
        return service.get(actor, id);
    }

    @PostMapping(value = EndPoint.ALERTS + "/{id}/accept", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('RANGER', 'LIAISON_OFFICER')")
    public ResponseEntity<StandardResponse<AlertResponseDTO>> accept(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id) {
        return service.accept(actor, id);
    }

    @PostMapping(value = EndPoint.ALERTS + "/{id}/decline", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('RANGER', 'LIAISON_OFFICER')")
    public ResponseEntity<StandardResponse<AlertDeclineResponseDTO>> decline(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody DeclineRequestDTO request) {
        return service.decline(actor, id, mapper.toRequest(request));
    }

    @PostMapping(value = EndPoint.ALERTS + "/{id}/support", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('RANGER', 'LIAISON_OFFICER')")
    public ResponseEntity<StandardResponse<AlertSupportResponseDTO>> support(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody SupportRequestDTO request) {
        return service.support(actor, id, mapper.toRequest(request));
    }

    @PutMapping(value = EndPoint.ALERTS + "/{id}/response", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('RANGER', 'LIAISON_OFFICER')")
    public ResponseEntity<StandardResponse<AlertResponseDTO>> resolve(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody AlertResponseRequestDTO request) {
        return service.resolve(actor, id, mapper.toRequest(request));
    }
}
