package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.CommunityReportRequest;
import com.wildlife.wildlife_conservationbackend.domain.CommunityResponseRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.CommunityReportQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.CommunityReportResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface CommunityReportService {
    ResponseEntity<StandardResponse<CommunityReportResponseDTO>> submit(CurrentUser actor, String id, CommunityReportRequest request);
    ResponseEntity<StandardResponse<PageResponse<CommunityReportResponseDTO>>> list(CurrentUser actor, CommunityReportQuery query, PageQuery page);
    ResponseEntity<StandardResponse<CommunityReportResponseDTO>> get(CurrentUser actor, String id);
    ResponseEntity<StandardResponse<CommunityReportResponseDTO>> accept(CurrentUser actor, String id);
    ResponseEntity<StandardResponse<CommunityReportResponseDTO>> resolve(CurrentUser actor, String id, CommunityResponseRequest request);
}
