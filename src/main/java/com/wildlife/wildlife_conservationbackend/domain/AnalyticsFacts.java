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
}
