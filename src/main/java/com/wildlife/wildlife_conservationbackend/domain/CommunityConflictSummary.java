package com.wildlife.wildlife_conservationbackend.domain;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityConflictSummary {
    private Map<String, Long> typeCounts = Map.of();
    private List<DailyCount> dailyCounts = List.of();
    private List<CommunityAreaCount> areaCounts = List.of();
}
