package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.StaffCreateRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.UserSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.UserMapper;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
import com.wildlife.wildlife_conservationbackend.service.impl.AccountCreator;
import com.wildlife.wildlife_conservationbackend.service.impl.UserServiceImpl;
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
class UserServiceTests {

    @Mock
    private MongoPageReader pageReader;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParkService parkService;
    @Mock
    private AccountCreator accountCreator;

    private UserServiceImpl userService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        userService = new UserServiceImpl(
                pageReader,
                new UserMapper(),
                userRepository,
                parkService,
                responseGenerator,
                accountCreator
        );
    }

    @Test
    void createStaff_Success() {
        CurrentUser admin = new CurrentUser("admin-id", Role.PARK_MANAGER, Set.of("park1"));
        StaffCreateRequest request = new StaffCreateRequest();
        request.setName("John Ranger");
        request.setEmail("john@example.com");
        request.setRole(Role.RANGER);
        request.setTemporaryPassword("Temp123!");
        request.setParkIds(Set.of("park1"));

        UserEntity newEntity = new UserEntity("new-user-id", "John Ranger", "john@example.com", "hash", Role.RANGER, Set.of("park1"), true);

        when(accountCreator.create("John Ranger", "john@example.com", "Temp123!", Role.RANGER, Set.of("park1"), true))
            .thenReturn(newEntity);

        ResponseEntity<StandardResponse<UserProfileResponseDTO>> response = userService.createStaff(admin, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getId()).isEqualTo("new-user-id");
        assertThat(response.getBody().getData().getName()).isEqualTo("John Ranger");
        assertThat(response.getBody().getData().getRole()).isEqualTo(Role.RANGER);
        verify(parkService).requireExistingPark(admin, "park1");
    }

    @Test
    void getProfile_Success() {
        CurrentUser actor = new CurrentUser("actor-id", Role.RANGER, Set.of("park1"));
        UserEntity user = new UserEntity("actor-id", "U1", "u1@e.com", "hash", Role.RANGER, Set.of("park1"), true);
        
        when(userRepository.findById("actor-id")).thenReturn(Optional.of(user));

        ResponseEntity<StandardResponse<UserProfileResponseDTO>> response = userService.getProfile(actor);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getId()).isEqualTo("actor-id");
    }

    @Test
    void listUsers_WithRoleFilter() {
        CurrentUser actor = new CurrentUser("actor-id", Role.PARK_MANAGER, Set.of("park1"));
        PageQuery pageQuery = new PageQuery(0, 10);

        when(parkService.accessibleParkIds(actor, null)).thenReturn(Set.of("park1"));

        UserEntity user1 = new UserEntity("u1", "U1", "u1@e.com", "hash", Role.RANGER, Set.of("park1"), true);
        when(pageReader.find(any(Criteria.class), any(PageRequest.class), eq(UserEntity.class)))
                .thenReturn(new PageImpl<>(List.of(user1)));

        ResponseEntity<StandardResponse<PageResponse<UserSummaryResponseDTO>>> response = 
                userService.listUsers(actor, null, Role.RANGER, pageQuery);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getItems()).hasSize(1);
        assertThat(response.getBody().getData().getItems().get(0).getName()).isEqualTo("U1");

        ArgumentCaptor<Criteria> criteriaCaptor = ArgumentCaptor.forClass(Criteria.class);
        verify(pageReader).find(criteriaCaptor.capture(), any(PageRequest.class), eq(UserEntity.class));
    }
}
