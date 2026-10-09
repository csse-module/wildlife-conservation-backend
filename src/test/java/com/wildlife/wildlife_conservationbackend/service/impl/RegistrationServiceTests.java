package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.config.CommunityRegistrationProperties;
import com.wildlife.wildlife_conservationbackend.domain.RegistrationRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.RegistrationParkResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.UserMapper;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.AccountCreator;
import com.wildlife.wildlife_conservationbackend.service.impl.RegistrationServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTests {

    @Mock
    private CommunityRegistrationProperties properties;
    @Mock
    private ParkRepository parkRepository;
    @Mock
    private AccountCreator accountCreator;
    @Mock
    private UserMapper userMapper;

    private RegistrationServiceImpl registrationService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        registrationService = new RegistrationServiceImpl(
                properties,
                parkRepository,
                accountCreator,
                userMapper,
                responseGenerator
        );
    }

    @Test
    void register_Success() {
        RegistrationRequest request = new RegistrationRequest("John", "john@example.com", "Password123!", "park1");

        when(properties.getCommunityParkIds()).thenReturn(Set.of("park1"));
        when(parkRepository.existsById("park1")).thenReturn(true);

        UserEntity userEntity = new UserEntity("new-user-id", "John", "john@example.com", "hash", Role.COMMUNITY_MEMBER, Set.of("park1"), false);
        when(accountCreator.create("John", "john@example.com", "Password123!", Role.COMMUNITY_MEMBER, Set.of("park1"), false))
                .thenReturn(userEntity);

        UserProfileResponseDTO mockResponse = new UserProfileResponseDTO();
        when(userMapper.toProfile(userEntity)).thenReturn(mockResponse);

        ResponseEntity<StandardResponse<UserProfileResponseDTO>> response = registrationService.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void register_ParkNotAllowed() {
        RegistrationRequest request = new RegistrationRequest("John", "john@example.com", "Password123!", "park2");

        when(properties.getCommunityParkIds()).thenReturn(Set.of("park1"));

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not enabled");
    }

    @Test
    void availableParks_Success() {
        when(properties.getCommunityParkIds()).thenReturn(Set.of("park1", "park2"));
        
        ParkEntity park1 = new ParkEntity("park1", "Yellowstone", "YNP", List.of());
        ParkEntity park2 = new ParkEntity("park2", "Yosemite", "YOS", List.of());
        when(parkRepository.findAllById(Set.of("park1", "park2"))).thenReturn(List.of(park1, park2));

        ResponseEntity<StandardResponse<List<RegistrationParkResponseDTO>>> response = registrationService.availableParks();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(2);
        assertThat(response.getBody().getData().get(0).getName()).isEqualTo("Yellowstone"); // Y before Yo
    }
}
