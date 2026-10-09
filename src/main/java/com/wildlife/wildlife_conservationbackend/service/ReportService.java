package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.ReportRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.ReportDetailResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.ReportSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface ReportService {
    ResponseEntity<StandardResponse<ReportDetailResponseDTO>> generate(CurrentUser actor, String id, ReportRequest request);
    ResponseEntity<StandardResponse<PageResponse<ReportSummaryResponseDTO>>> list(CurrentUser actor, String parkId, PageQuery page);
    ResponseEntity<StandardResponse<ReportDetailResponseDTO>> get(CurrentUser actor, String id);
    ResponseEntity<byte[]> download(CurrentUser actor, String id);
}
