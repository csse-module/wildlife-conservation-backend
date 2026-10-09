package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.AnalyticsSnapshot;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.ReportRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.ReportDetailResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.ReportEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.mapper.ReportMapper;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.ReportRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.ReportServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.ReportPdfRenderer;
import com.wildlife.wildlife_conservationbackend.utility.RequestFingerprint;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTests {

    @Mock
    private ReportRepository repository;
    @Mock
    private ReportMapper mapper;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private ParkService parkService;
    @Mock
    private MongoPageReader pageReader;
    @Mock
    private RequestFingerprint requestFingerprint;
    @Mock
    private ReportPdfRenderer pdfRenderer;

    private ReportServiceImpl reportService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        reportService = new ReportServiceImpl(
                repository,
                mapper,
                analyticsService,
                parkService,
                pageReader,
                requestFingerprint,
                responseGenerator,
                pdfRenderer
        );
    }

    @Test
    void generate_Success() {
        CurrentUser manager = new CurrentUser("manager-id", Role.PARK_MANAGER, Set.of("park1"));
        ReportRequest request = new ReportRequest("park1", com.wildlife.wildlife_conservationbackend.enums.ReportType.MONTHLY_CONSERVATION, LocalDate.now().minusDays(30), LocalDate.now(), List.of(com.wildlife.wildlife_conservationbackend.enums.ReportSection.INCIDENT_STATISTICS));
        
        when(requestFingerprint.of(any())).thenReturn("hash");
        when(repository.findById("new-report")).thenReturn(Optional.empty());
        
        AnalyticsSnapshot snapshot = new AnalyticsSnapshot("park1", LocalDate.now().minusDays(30), LocalDate.now(), true, 0, List.of(), List.of(), List.of(), null, 0, 0, Instant.now());
        when(analyticsService.snapshot(any(), any(), any(), any())).thenReturn(snapshot);
        
        ReportEntity savedEntity = ReportEntity.builder().id("new-report").build();
        when(repository.insert(any(ReportEntity.class))).thenReturn(savedEntity);
        
        ReportDetailResponseDTO mockResponse = new ReportDetailResponseDTO();
        when(mapper.toResponse(savedEntity)).thenReturn(mockResponse);

        ResponseEntity<StandardResponse<ReportDetailResponseDTO>> response = 
                reportService.generate(manager, "new-report", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
