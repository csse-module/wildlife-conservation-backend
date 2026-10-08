package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.PatrolAssignmentRequest;
import com.wildlife.wildlife_conservationbackend.domain.PatrolRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.AssignmentQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolAssignmentResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolRouteResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface PatrolService {
    ResponseEntity<StandardResponse<PageResponse<PatrolRouteResponseDTO>>> listRoutes(
            CurrentUser actor, String parkId, PageQuery page);
    ResponseEntity<StandardResponse<PatrolRouteResponseDTO>> getRoute(CurrentUser actor, String id);
    ResponseEntity<StandardResponse<PatrolAssignmentResponseDTO>> assign(
            CurrentUser actor, String id, PatrolAssignmentRequest request);
    ResponseEntity<StandardResponse<PageResponse<PatrolAssignmentResponseDTO>>> listAssignments(
            CurrentUser actor, AssignmentQuery query, PageQuery page);
    ResponseEntity<StandardResponse<PatrolResponseDTO>> complete(CurrentUser actor, String id, PatrolRequest request);
    ResponseEntity<StandardResponse<PageResponse<PatrolSummaryResponseDTO>>> listPatrols(
            CurrentUser actor, PatrolQuery query, PageQuery page);
    ResponseEntity<StandardResponse<PatrolResponseDTO>> getPatrol(CurrentUser actor, String id);
}
