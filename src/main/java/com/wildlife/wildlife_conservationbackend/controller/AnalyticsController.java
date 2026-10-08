package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.AnalyticsQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.AnalyticsSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.service.AnalyticsService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EndPoint.BASE)
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService service;

    @GetMapping(value = EndPoint.ANALYTICS, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PARK_MANAGER')")
    public ResponseEntity<StandardResponse<AnalyticsSummaryResponseDTO>> summary(
            @AuthenticationPrincipal CurrentUser actor,
            @Valid @ModelAttribute AnalyticsQuery query) {
        return service.summary(actor, query);
    }
}
