package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.IncidentRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.IncidentQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.IncidentResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface IncidentService {
    ResponseEntity<StandardResponse<IncidentResponseDTO>> submit(CurrentUser actor, String id, IncidentRequest request);
    ResponseEntity<StandardResponse<PageResponse<IncidentResponseDTO>>> list(CurrentUser actor, IncidentQuery query, PageQuery page);
    ResponseEntity<StandardResponse<IncidentResponseDTO>> get(CurrentUser actor, String id);
}
