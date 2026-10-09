package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.AnalyticsSnapshot;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.AnalyticsQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.AnalyticsSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;

public interface AnalyticsService {
    ResponseEntity<StandardResponse<AnalyticsSummaryResponseDTO>> summary(CurrentUser actor, AnalyticsQuery query);
    AnalyticsSnapshot snapshot(CurrentUser actor, String parkId, LocalDate from, LocalDate to);
}
