package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.ParkCreateRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.ParkResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.ParkArea;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.ParkMapper;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.ParkServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkServiceTests {

    @Mock
    private MongoPageReader pageReader;
    @Mock
    private ParkMapper parkMapper;
    @Mock
    private ParkRepository parkRepository;
    @Mock
    private UserRepository userRepository;

    private ParkServiceImpl parkService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        parkService = new ParkServiceImpl(
                pageReader,
                parkMapper,
                responseGenerator,
                parkRepository,
                userRepository
        );
    }

    @Test
    void listParks_Success() {
        CurrentUser actor = new CurrentUser("actor-id", Role.PARK_MANAGER, Set.of("park1"));
        PageQuery pageQuery = new PageQuery(0, 10);
        
        ParkEntity park = new ParkEntity("park1", "Yellowstone", "YNP", List.of());
        when(pageReader.find(any(Criteria.class), any(PageRequest.class), eq(ParkEntity.class)))
                .thenReturn(new PageImpl<>(List.of(park)));

        when(parkMapper.toResponse(any())).thenReturn(new ParkResponseDTO());

        ResponseEntity<StandardResponse<PageResponse<ParkResponseDTO>>> response = parkService.listParks(actor, pageQuery);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getItems()).hasSize(1);
    }

    @Test
    void requirePark_Success() {
        CurrentUser actor = new CurrentUser("actor-id", Role.PARK_MANAGER, Set.of("park1"));
        parkService.requirePark(actor, "park1"); // Should not throw exception
    }

    @Test
    void requirePark_Failure() {
        CurrentUser actor = new CurrentUser("actor-id", Role.PARK_MANAGER, Set.of("park1"));
        assertThatThrownBy(() -> parkService.requirePark(actor, "park2"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("access");
    }

    @Test
    void requireArea_Success() {
        CurrentUser actor = new CurrentUser("actor-id", Role.PARK_MANAGER, Set.of("park1"));
        ParkArea area = new ParkArea("area1", "North Area");
        ParkEntity park = new ParkEntity("park1", "Yellowstone", "YNP", List.of(area));
        
        when(parkRepository.findById("park1")).thenReturn(Optional.of(park));
        
        parkService.requireArea(actor, "park1", "area1"); // Should not throw exception
    }

    @Test
    void requireArea_Failure() {
        CurrentUser actor = new CurrentUser("actor-id", Role.PARK_MANAGER, Set.of("park1"));
        ParkEntity park = new ParkEntity("park1", "Yellowstone", "YNP", List.of());
        
        when(parkRepository.findById("park1")).thenReturn(Optional.of(park));
        
        assertThatThrownBy(() -> parkService.requireArea(actor, "park1", "area2"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not belong to this park");
    }
}
