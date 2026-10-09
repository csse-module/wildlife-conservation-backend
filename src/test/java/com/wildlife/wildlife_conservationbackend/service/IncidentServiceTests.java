package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.IncidentRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.IncidentResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.IncidentEntity;
import com.wildlife.wildlife_conservationbackend.enums.IncidentType;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.IncidentMapper;
import com.wildlife.wildlife_conservationbackend.repository.IncidentRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.PatrolAssignmentRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.IncidentServiceImpl;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTests {

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private IncidentMapper incidentMapper;
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
    private PatrolAssignmentRepository assignmentRepository;
    @Mock
    private Clock clock;

    private IncidentServiceImpl incidentService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        incidentService = new IncidentServiceImpl(
                incidentRepository,
                incidentMapper,
                parkService,
                mediaService,
                pageReader,
                dateRangeResolver,
                observationValidator,
                requestFingerprint,
                responseGenerator,
                clock,
                assignmentRepository
        );
    }

    @Test
    void submit_Success() {
        CurrentUser ranger = new CurrentUser("ranger-id", Role.RANGER, Set.of("park1"));
        IncidentRequest request = new IncidentRequest("park1", "area1", null, IncidentType.POACHING_EVIDENCE, Instant.now(), null, "desc", null);
        
        when(requestFingerprint.of(any())).thenReturn("hash");
        when(incidentRepository.findById("new-inc")).thenReturn(Optional.empty());
        when(clock.instant()).thenReturn(Instant.now());
        
        IncidentEntity savedEntity = IncidentEntity.builder().id("new-inc").build();
        when(incidentRepository.insert(any(IncidentEntity.class))).thenReturn(savedEntity);
        
        IncidentResponseDTO mockResponse = new IncidentResponseDTO();
        when(incidentMapper.toResponse(savedEntity)).thenReturn(mockResponse);

        ResponseEntity<StandardResponse<IncidentResponseDTO>> response = 
                incidentService.submit(ranger, "new-inc", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(parkService).requireArea(ranger, "park1", "area1");
    }

    @Test
    void get_Success() {
        CurrentUser manager = new CurrentUser("manager-id", Role.PARK_MANAGER, Set.of("park1"));
        IncidentEntity entity = IncidentEntity.builder().id("inc-1").parkId("park1").build();
        
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(entity));

        ResponseEntity<StandardResponse<IncidentResponseDTO>> response = 
                incidentService.get(manager, "inc-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(parkService).requirePark(manager, "park1");
    }

    @Test
    void get_NotFound() {
        CurrentUser manager = new CurrentUser("manager-id", Role.PARK_MANAGER, Set.of("park1"));
        
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> incidentService.get(manager, "inc-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Incident");
    }
}
