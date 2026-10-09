package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.AnalyticsSnapshot;
import com.wildlife.wildlife_conservationbackend.dto.response.AnalyticsSummaryResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsMapper {
    public AnalyticsSummaryResponseDTO toResponse(AnalyticsSnapshot snapshot) {
        return new AnalyticsSummaryResponseDTO(snapshot.getParkId(), snapshot.getFrom(), snapshot.getTo(),
                snapshot.isDataAvailable(), snapshot.getTotalIncidents(), snapshot.getIncidentTypeCounts(),
                snapshot.getDailyIncidentCounts(), snapshot.getAreaCounts(), snapshot.getPatrolCoverage(),
                snapshot.getCommunityReportCount(), snapshot.getResolvedAlertCount(), snapshot.getGeneratedAt(), snapshot.getCommunityConflict());
    }
}
