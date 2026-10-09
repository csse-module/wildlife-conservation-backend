package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.AlertSetupRequest;
import com.wildlife.wildlife_conservationbackend.domain.AlertResponseRequest;
import com.wildlife.wildlife_conservationbackend.domain.DeclineRequest;
import com.wildlife.wildlife_conservationbackend.domain.SupportRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.AlertQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertDeclineResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertSupportResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface AlertService {
    ResponseEntity<StandardResponse<AlertResponseDTO>> create(CurrentUser actor, String id, AlertSetupRequest request);
    ResponseEntity<StandardResponse<PageResponse<AlertResponseDTO>>> list(CurrentUser actor, AlertQuery query, PageQuery page);
    ResponseEntity<StandardResponse<AlertResponseDTO>> get(CurrentUser actor, String id);
    ResponseEntity<StandardResponse<AlertResponseDTO>> accept(CurrentUser actor, String id);
    ResponseEntity<StandardResponse<AlertDeclineResponseDTO>> decline(CurrentUser actor, String id, DeclineRequest request);
    ResponseEntity<StandardResponse<AlertSupportResponseDTO>> support(CurrentUser actor, String id, SupportRequest request);
    ResponseEntity<StandardResponse<AlertResponseDTO>> resolve(CurrentUser actor, String id, AlertResponseRequest request);
}
