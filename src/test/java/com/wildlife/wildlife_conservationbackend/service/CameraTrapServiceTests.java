package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CameraImageRequest;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraImageResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.CameraImageEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraTrapEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.mapper.CameraTrapMapper;
import com.wildlife.wildlife_conservationbackend.repository.CameraImageRepository;
import com.wildlife.wildlife_conservationbackend.repository.CameraImageTransitionRepository;
import com.wildlife.wildlife_conservationbackend.repository.CameraTrapRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.service.impl.CameraTrapServiceImpl;
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
class CameraTrapServiceTests {

    @Mock
    private CameraImageRepository repository;
    @Mock
    private CameraTrapRepository cameraTrapRepository;
    @Mock
    private CameraImageTransitionRepository transitions;
    @Mock
    private CameraTrapMapper mapper;
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

    private CameraTrapServiceImpl cameraTrapService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        cameraTrapService = new CameraTrapServiceImpl(
                repository,
                cameraTrapRepository,
                transitions,
                mapper,
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
    void submit_Success() {
        CurrentUser researcher = new CurrentUser("researcher-id", Role.RESEARCHER, Set.of("park1"));
        CameraImageRequest request = new CameraImageRequest("trap-1", "media-1", Instant.now());
        
        when(requestFingerprint.of(any())).thenReturn("hash");
        when(repository.findById("image-1")).thenReturn(Optional.empty());
        
        CameraTrapEntity trap = new CameraTrapEntity("trap-1", "park1", "area1", "Trap 1", null);
        when(cameraTrapRepository.findById("trap-1")).thenReturn(Optional.of(trap));
        when(clock.instant()).thenReturn(Instant.now());
        
        CameraImageEntity savedEntity = CameraImageEntity.builder().id("image-1").build();
        when(repository.insert(any(CameraImageEntity.class))).thenReturn(savedEntity);
        
        CameraImageResponseDTO mockResponse = new CameraImageResponseDTO();
        when(mapper.toResponse(savedEntity)).thenReturn(mockResponse);

        ResponseEntity<StandardResponse<CameraImageResponseDTO>> response = 
                cameraTrapService.submit(researcher, "image-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(parkService).requireArea(researcher, "park1", "area1");
    }

    @Test
    void get_Success() {
        CurrentUser manager = new CurrentUser("manager-id", Role.PARK_MANAGER, Set.of("park1"));
        CameraImageEntity entity = CameraImageEntity.builder().id("image-1").parkId("park1").build();
        
        when(repository.findById("image-1")).thenReturn(Optional.of(entity));

        ResponseEntity<StandardResponse<CameraImageResponseDTO>> response = 
                cameraTrapService.get(manager, "image-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(parkService).requirePark(manager, "park1");
    }
}
