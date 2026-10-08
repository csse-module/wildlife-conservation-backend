package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.CommunityReportQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.CommunityReportRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.CommunityReportResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.CommunityReportMapper;
import com.wildlife.wildlife_conservationbackend.service.CommunityReportService;
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
public class CommunityReportController {
    private final CommunityReportService service;
    private final CommunityReportMapper mapper;

    @PutMapping(value = EndPoint.COMMUNITY_REPORTS + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('COMMUNITY_MEMBER')")
    public ResponseEntity<StandardResponse<CommunityReportResponseDTO>> submit(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id,
            @Valid @RequestBody CommunityReportRequestDTO request) {
        return service.submit(actor, id, mapper.toRequest(request));
    }

    @GetMapping(value = EndPoint.COMMUNITY_REPORTS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('COMMUNITY_MEMBER', 'PARK_MANAGER', 'LIAISON_OFFICER')")
    public ResponseEntity<StandardResponse<PageResponse<CommunityReportResponseDTO>>> list(
            @AuthenticationPrincipal CurrentUser actor,
            @Valid @ModelAttribute CommunityReportQuery query,
            @Valid @ModelAttribute PageQuery page) {
        return service.list(actor, query, page);
    }

    @GetMapping(value = EndPoint.COMMUNITY_REPORTS + "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('COMMUNITY_MEMBER', 'PARK_MANAGER', 'LIAISON_OFFICER')")
    public ResponseEntity<StandardResponse<CommunityReportResponseDTO>> get(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable @Pattern(regexp = ValidationPatterns.UUID) String id) {
        return service.get(actor, id);
    }
}
