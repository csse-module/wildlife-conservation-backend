package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.domain.AreaCount;
import com.wildlife.wildlife_conservationbackend.domain.DailyCount;
import com.wildlife.wildlife_conservationbackend.domain.IncidentTypeCount;
import com.wildlife.wildlife_conservationbackend.domain.RouteCoverage;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponseDTO {
    private String parkId;

    private LocalDate from;

    private LocalDate to;

    private boolean dataAvailable;

    private long totalIncidents;

    private List<IncidentTypeCount> incidentTypeCounts;

    private List<DailyCount> dailyIncidentCounts;

    private List<AreaCount> areaCounts;

    private RouteCoverage patrolCoverage;

    private long communityReportCount;

    private long resolvedAlertCount;

    private Instant generatedAt;
}
