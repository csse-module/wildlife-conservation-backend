package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CommunityReportRequest;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.dto.response.CommunityReportResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportType;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.mapper.CommunityReportMapper;
import com.wildlife.wildlife_conservationbackend.repository.CommunityReportRepository;
import com.wildlife.wildlife_conservationbackend.repository.CommunityReportTransitionRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.service.impl.CommunityReportServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.DateRangeResolver;
import com.wildlife.wildlife_conservationbackend.utility.ObservationValidator;
import com.wildlife.wildlife_conservationbackend.utility.RequestFingerprint;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityReportServiceTests {

    @Mock
    private CommunityReportRepository repository;
    @Mock
    private CommunityReportMapper mapper;
    @Mock
    private ParkService parkService;
    @Mock
    private MediaService mediaService;
    @Mock
    private MongoPageReader pageReader;
    @Mock
    private DateRangeResolver dateRangeResolver;
    @Mock
    private ObservationValidator observationValidator;
    @Mock
    private RequestFingerprint requestFingerprint;
    @Mock
    private Clock clock;
    @Mock
    private CommunityReportTransitionRepository transitions;

    private CommunityReportServiceImpl communityReportService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        communityReportService = new CommunityReportServiceImpl(
                repository,
                mapper,
                parkService,
                mediaService,
                pageReader,
                dateRangeResolver,
                observationValidator,
                requestFingerprint,
                responseGenerator,
                clock,
                transitions
        );
    }

    @Test
    void submit_Success() {
        CurrentUser member = new CurrentUser("member-id", Role.COMMUNITY_MEMBER, Set.of("park1"));
        CommunityReportRequest request = new CommunityReportRequest("park1", "area1", CommunityReportType.WILDLIFE_SIGHTING, "Lion", "VillageA", Instant.now(), null, "Saw lion", null, null);
        
        when(requestFingerprint.of(any())).thenReturn("hash");
        when(repository.findById("new-report")).thenReturn(Optional.empty());
        when(clock.instant()).thenReturn(Instant.now());
        
        CommunityReportEntity savedEntity = CommunityReportEntity.builder().id("new-report").build();
        when(repository.insert(any(CommunityReportEntity.class))).thenReturn(savedEntity);
        
        CommunityReportResponseDTO mockResponse = new CommunityReportResponseDTO();
        when(mapper.toResponse(savedEntity)).thenReturn(mockResponse);

        ResponseEntity<StandardResponse<CommunityReportResponseDTO>> response = 
                communityReportService.submit(member, "new-report", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(parkService).requireArea(member, "park1", "area1");
    }

    @Test
    void get_Success() {
        CurrentUser manager = new CurrentUser("manager-id", Role.PARK_MANAGER, Set.of("park1"));
        CommunityReportEntity entity = CommunityReportEntity.builder().id("report-1").parkId("park1").reportedBy("member-id").build();
        
        when(repository.findById("report-1")).thenReturn(Optional.of(entity));

        ResponseEntity<StandardResponse<CommunityReportResponseDTO>> response = 
                communityReportService.get(manager, "report-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(parkService).requirePark(manager, "park1");
    }
}
