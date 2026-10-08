package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolRouteResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.service.PatrolService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = EndPoint.BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PARK_MANAGER', 'RANGER')")
public class PatrolRouteController {
    private final PatrolService patrolService;

    @GetMapping(EndPoint.ROUTES)
    public ResponseEntity<StandardResponse<PageResponse<PatrolRouteResponseDTO>>> list(
            @AuthenticationPrincipal CurrentUser actor, @RequestParam(required = false) @Size(max = 100) String parkId,
            @Valid @ModelAttribute PageQuery page) {
        return patrolService.listRoutes(actor, parkId, page);
    }

    @GetMapping(EndPoint.ROUTES + "/{id}")
    public ResponseEntity<StandardResponse<PatrolRouteResponseDTO>> get(
            @AuthenticationPrincipal CurrentUser actor, @PathVariable @Size(min = 1, max = 100) String id) {
        return patrolService.getRoute(actor, id);
    }
}
