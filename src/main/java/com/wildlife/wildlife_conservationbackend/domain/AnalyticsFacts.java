package com.wildlife.wildlife_conservationbackend.domain;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsFacts {
    private Map<String, Long> incidentTypeCounts;
    private Map<String, Long> dailyIncidentCounts;
    private Map<String, Long> areaCounts;
    private long communityReportCount;
    private long resolvedAlertCount;
    private long completedPatrolCount;
    private long assignedRouteCount;
    private long completedRouteCount;

    private Map<String, Long> communityTypeCounts = Map.of();
    private Map<String, Long> dailyCommunityCounts = Map.of();
    private Map<String, Long> communityAreaCounts = Map.of();

    public AnalyticsFacts(Map<String, Long> incidentTypeCounts, Map<String, Long> dailyIncidentCounts,
                          Map<String, Long> areaCounts, long communityReportCount, long resolvedAlertCount,
                          long completedPatrolCount, long assignedRouteCount, long completedRouteCount) {
        this.incidentTypeCounts = incidentTypeCounts;
        this.dailyIncidentCounts = dailyIncidentCounts;
        this.areaCounts = areaCounts;
        this.communityReportCount = communityReportCount;
        this.resolvedAlertCount = resolvedAlertCount;
        this.completedPatrolCount = completedPatrolCount;
        this.assignedRouteCount = assignedRouteCount;
        this.completedRouteCount = completedRouteCount;
    }
}
