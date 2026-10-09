package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.AnalyticsFacts;
import com.wildlife.wildlife_conservationbackend.domain.AnalyticsSnapshot;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.DateRange;
import com.wildlife.wildlife_conservationbackend.dto.request.AnalyticsQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.AnalyticsSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.mapper.AnalyticsMapper;
import com.wildlife.wildlife_conservationbackend.repository.AnalyticsRepository;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.AnalyticsServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.DateRangeResolver;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTests {

    @Mock
    private ParkService parkService;
    @Mock
    private ParkRepository parkRepository;
    @Mock
    private DateRangeResolver dateRangeResolver;
    @Mock
    private AnalyticsRepository analyticsRepository;
    @Mock
    private AnalyticsMapper mapper;
    @Mock
    private Clock clock;

    private AnalyticsServiceImpl analyticsService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        analyticsService = new AnalyticsServiceImpl(
                parkService,
                parkRepository,
                dateRangeResolver,
                analyticsRepository,
                mapper,
                responseGenerator,
                clock
        );
    }

    @Test
    void summary_Success() {
        CurrentUser manager = new CurrentUser("manager-id", Role.PARK_MANAGER, Set.of("park1"));
        AnalyticsQuery query = new AnalyticsQuery("park1", LocalDate.now().minusDays(7), LocalDate.now());

        DateRange range = new DateRange(Instant.now(), Instant.now(), "label");
        when(dateRangeResolver.resolve(any(), any(), any())).thenReturn(range);
        
        ParkEntity park = new ParkEntity("park1", "Park", "PK", List.of());
        when(parkRepository.findById("park1")).thenReturn(Optional.of(park));
        
        AnalyticsFacts facts = new AnalyticsFacts(Map.of(), Map.of(), Map.of(), 0, 0, 0, 0, 0, Map.of(), Map.of(), Map.of());
        when(analyticsRepository.summarize("park1", range)).thenReturn(facts);
        when(clock.instant()).thenReturn(Instant.now());
        when(mapper.toResponse(any())).thenReturn(new AnalyticsSummaryResponseDTO());

        ResponseEntity<StandardResponse<AnalyticsSummaryResponseDTO>> response = analyticsService.summary(manager, query);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(parkService).requirePark(manager, "park1");
    }
}
