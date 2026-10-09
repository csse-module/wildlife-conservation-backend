package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.ParkQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.ReportRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.ReportDetailResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.ReportSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.ReportMapper;
import com.wildlife.wildlife_conservationbackend.service.ReportService;
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
public class ReportController {
    private final ReportService service;
    private final ReportMapper mapper;

    @PutMapping(value = EndPoint.REPORTS + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RESEARCHER')")
    public ResponseEntity<StandardResponse<ReportDetailResponseDTO>> generate(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody ReportRequestDTO request) {
        return service.generate(actor, id, mapper.toRequest(request));
    }

    @GetMapping(value = EndPoint.REPORTS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RESEARCHER')")
    public ResponseEntity<StandardResponse<PageResponse<ReportSummaryResponseDTO>>> list(
            @AuthenticationPrincipal CurrentUser actor,
            @Valid @ModelAttribute ParkQuery query,
            @Valid @ModelAttribute PageQuery page) {
        return service.list(actor, query.getParkId(), page);
    }

    @GetMapping(value = EndPoint.REPORTS + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RESEARCHER')")
    public ResponseEntity<StandardResponse<ReportDetailResponseDTO>> get(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id) {
        return service.get(actor, id);
    }

    @GetMapping(value = EndPoint.REPORTS + "/{id}/download")
    @PreAuthorize("hasAnyRole('PARK_MANAGER', 'RESEARCHER')")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id) {
        return service.download(actor, id);
    }
}
