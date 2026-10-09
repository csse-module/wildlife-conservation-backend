package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.MediaUploadRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.MediaResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.MediaEntity;
import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.mapper.MediaMapper;
import com.wildlife.wildlife_conservationbackend.repository.MediaAccessRepository;
import com.wildlife.wildlife_conservationbackend.repository.MediaRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.MediaServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.ImageValidator;
import com.wildlife.wildlife_conservationbackend.utility.MediaStorage;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTests {

    @Mock
    private MediaRepository repository;
    @Mock
    private MediaMapper mapper;
    @Mock
    private ParkService parkService;
    @Mock
    private MediaAccessRepository accessRepository;
    @Mock
    private MediaStorage storage;
    @Mock
    private ImageValidator imageValidator;
    @Mock
    private Clock clock;

    private MediaServiceImpl mediaService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        mediaService = new MediaServiceImpl(
                repository,
                mapper,
                parkService,
                accessRepository,
                storage,
                imageValidator,
                responseGenerator,
                clock
        );
    }

    @Test
    void upload_Success() {
        CurrentUser manager = new CurrentUser("manager-id", Role.PARK_MANAGER, Set.of("park1"));
        MediaUploadRequest request = new MediaUploadRequest("park1", MediaCategory.CAMERA_TRAP);
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "dummy content".getBytes());
        
        when(repository.findById("new-media")).thenReturn(Optional.empty());
        when(storage.store(anyString(), any(), anyString())).thenReturn("key");
        when(clock.instant()).thenReturn(Instant.now());
        
        MediaEntity savedEntity = MediaEntity.builder().id("new-media").build();
        when(repository.insert(any(MediaEntity.class))).thenReturn(savedEntity);
        
        MediaResponseDTO mockResponse = new MediaResponseDTO();
        when(mapper.toResponse(savedEntity)).thenReturn(mockResponse);

        ResponseEntity<StandardResponse<MediaResponseDTO>> response = 
                mediaService.upload(manager, "new-media", request, file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(parkService).requireExistingPark(manager, "park1");
    }
}
