package com.wildlife.wildlife_conservationbackend.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSnapshot {
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

    private CommunityConflictSummary communityConflict = new CommunityConflictSummary();

    public AnalyticsSnapshot(String parkId, LocalDate from, LocalDate to, boolean dataAvailable, long totalIncidents,
                             List<IncidentTypeCount> incidentTypeCounts, List<DailyCount> dailyIncidentCounts,
                             List<AreaCount> areaCounts, RouteCoverage patrolCoverage, long communityReportCount,
                             long resolvedAlertCount, Instant generatedAt) {
        this.parkId = parkId;
        this.from = from;
        this.to = to;
        this.dataAvailable = dataAvailable;
        this.totalIncidents = totalIncidents;
        this.incidentTypeCounts = incidentTypeCounts;
        this.dailyIncidentCounts = dailyIncidentCounts;
        this.areaCounts = areaCounts;
        this.patrolCoverage = patrolCoverage;
        this.communityReportCount = communityReportCount;
        this.resolvedAlertCount = resolvedAlertCount;
        this.generatedAt = generatedAt;
    }
}
