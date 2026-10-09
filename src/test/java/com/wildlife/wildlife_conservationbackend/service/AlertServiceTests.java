package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.AlertSetupRequest;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.AlertEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.mapper.AlertMapper;
import com.wildlife.wildlife_conservationbackend.repository.AlertRepository;
import com.wildlife.wildlife_conservationbackend.repository.AlertTransitionRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.service.impl.AlertServiceImpl;
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
class AlertServiceTests {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertTransitionRepository alertTransitionRepository;
    @Mock
    private AlertMapper alertMapper;
    @Mock
    private ParkService parkService;
    @Mock
    private MediaService mediaService;
    @Mock
    private MongoPageReader pageReader;
    @Mock
    private ObservationValidator observationValidator;
    @Mock
    private RequestFingerprint requestFingerprint;
    @Mock
    private Clock clock;

    private AlertServiceImpl alertService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        alertService = new AlertServiceImpl(
                alertRepository,
                alertTransitionRepository,
                alertMapper,
                parkService,
                mediaService,
                pageReader,
                observationValidator,
                requestFingerprint,
                responseGenerator,
                clock
        );
    }

    @Test
    void create_Success() {
        CurrentUser manager = new CurrentUser("manager-id", Role.PARK_MANAGER, Set.of("park1"));
        AlertSetupRequest request = new AlertSetupRequest("park1", "area1", "Lion", "Collar1", com.wildlife.wildlife_conservationbackend.enums.RiskLevel.HIGH, null, Instant.now(), Instant.now());
        
        when(requestFingerprint.of(any())).thenReturn("hash");
        when(alertRepository.findById("new-alert")).thenReturn(Optional.empty());
        when(clock.instant()).thenReturn(Instant.now());
        
        AlertEntity savedEntity = AlertEntity.builder().id("new-alert").build();
        when(alertRepository.insert(any(AlertEntity.class))).thenReturn(savedEntity);
        
        AlertResponseDTO mockResponse = new AlertResponseDTO();
        when(alertMapper.toResponse(savedEntity)).thenReturn(mockResponse);

        ResponseEntity<StandardResponse<AlertResponseDTO>> response = 
                alertService.create(manager, "new-alert", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(parkService).requireArea(manager, "park1", "area1");
    }

    @Test
    void get_Success() {
        CurrentUser manager = new CurrentUser("manager-id", Role.PARK_MANAGER, Set.of("park1"));
        AlertEntity entity = AlertEntity.builder().id("alert-1").parkId("park1").build();
        
        when(alertRepository.findById("alert-1")).thenReturn(Optional.of(entity));

        ResponseEntity<StandardResponse<AlertResponseDTO>> response = 
                alertService.get(manager, "alert-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(parkService).requirePark(manager, "park1");
    }
}
