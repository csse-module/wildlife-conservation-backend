package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.AnalyticsFacts;
import com.wildlife.wildlife_conservationbackend.domain.AnalyticsSnapshot;
import com.wildlife.wildlife_conservationbackend.domain.AreaCount;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.DailyCount;
import com.wildlife.wildlife_conservationbackend.domain.IncidentTypeCount;
import com.wildlife.wildlife_conservationbackend.domain.RouteCoverage;
import com.wildlife.wildlife_conservationbackend.dto.request.AnalyticsQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.AnalyticsSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.enums.IncidentType;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.AnalyticsMapper;
import com.wildlife.wildlife_conservationbackend.repository.AnalyticsRepository;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.service.AnalyticsService;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
import com.wildlife.wildlife_conservationbackend.utility.DateRangeResolver;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {
    private final ParkService parkService;
    private final ParkRepository parkRepository;
    private final DateRangeResolver dateRangeResolver;
    private final AnalyticsRepository repository;
    private final AnalyticsMapper mapper;
    private final ResponseGenerator responseGenerator;
    private final Clock clock;

    @Override
    public ResponseEntity<StandardResponse<AnalyticsSummaryResponseDTO>> summary(CurrentUser actor, AnalyticsQuery query) {
        var result = snapshot(actor, query.getParkId(), query.getFrom(), query.getTo());
        return responseGenerator.generateSuccessResponse(mapper.toResponse(result), HttpStatus.OK);
    }

    @Override
    public AnalyticsSnapshot snapshot(CurrentUser actor, String parkId, LocalDate from, LocalDate to) {
        parkService.requirePark(actor, parkId);
        var range = dateRangeResolver.resolve(parkId, from, to);
        var park = parkRepository.findById(parkId).orElseThrow(() -> ApiException.notFound("Park"));
        AnalyticsFacts facts = repository.summarize(parkId, range);
        List<IncidentTypeCount> types = facts.getIncidentTypeCounts().entrySet().stream()
                .map(entry -> new IncidentTypeCount(IncidentType.valueOf(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(count -> count.getType().name())).toList();
        List<DailyCount> days = from.datesUntil(to.plusDays(1))
                .map(date -> new DailyCount(date, facts.getDailyIncidentCounts().getOrDefault(date.toString(), 0L))).toList();
        Map<String, String> names = park.getAreas().stream().collect(Collectors.toMap(area -> area.getId(), area -> area.getName()));
        List<AreaCount> areas = facts.getAreaCounts().entrySet().stream()
                .map(entry -> new AreaCount(entry.getKey(), names.getOrDefault(entry.getKey(), "Unknown area"), entry.getValue(), entry.getValue() >= 3))
                .sorted(Comparator.comparingLong(AreaCount::getIncidentCount).reversed().thenComparing(AreaCount::getAreaId)).toList();
        long totalIncidents = types.stream().mapToLong(IncidentTypeCount::getCount).sum();
        RouteCoverage coverage = coverage(facts);
        boolean available = totalIncidents > 0 || facts.getCommunityReportCount() > 0 || facts.getResolvedAlertCount() > 0
                || facts.getCompletedPatrolCount() > 0 || facts.getAssignedRouteCount() > 0;
        log.debug("Analytics calculated parkId={} actorId={} dataAvailable={}", parkId, actor.getId(), available);
        return new AnalyticsSnapshot(parkId, from, to, available, totalIncidents, types, days, areas, coverage,
                facts.getCommunityReportCount(), facts.getResolvedAlertCount(), clock.instant());
    }

    private RouteCoverage coverage(AnalyticsFacts facts) {
        Double percent = facts.getAssignedRouteCount() == 0 ? null
                : Math.round(10000.0 * facts.getCompletedRouteCount() / facts.getAssignedRouteCount()) / 100.0;
        return new RouteCoverage("ASSIGNED_ROUTES_COMPLETED", facts.getAssignedRouteCount(), facts.getCompletedRouteCount(), percent);
    }
}
