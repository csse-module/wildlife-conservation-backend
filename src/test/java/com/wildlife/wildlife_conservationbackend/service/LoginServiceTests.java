package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.LoginRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.LoginResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.UserMapper;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.LoginServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.JwtTokenProvider;
import com.wildlife.wildlife_conservationbackend.utility.PasswordVerifier;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordVerifier passwordVerifier;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserMapper userMapper;

    private LoginServiceImpl loginService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        loginService = new LoginServiceImpl(
                userRepository,
                passwordVerifier,
                jwtTokenProvider,
                userMapper,
                responseGenerator
        );
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");
        UserEntity userEntity = new UserEntity("user-id", "User", "user@example.com", "hash", Role.RANGER, Set.of("park1"), true);
        
        when(userRepository.findByNormalizedEmail("user@example.com")).thenReturn(Optional.of(userEntity));
        when(passwordVerifier.matches("Password123!", "hash")).thenReturn(true);
        when(jwtTokenProvider.issue(userEntity)).thenReturn("token");
        when(jwtTokenProvider.expiresIn()).thenReturn(3600L);
        when(userMapper.toProfile(userEntity)).thenReturn(new UserProfileResponseDTO());

        ResponseEntity<StandardResponse<LoginResponseDTO>> response = loginService.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("token");
    }

    @Test
    void login_InvalidCredentials() {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword!");
        UserEntity userEntity = new UserEntity("user-id", "User", "user@example.com", "hash", Role.RANGER, Set.of("park1"), true);

        when(userRepository.findByNormalizedEmail("user@example.com")).thenReturn(Optional.of(userEntity));
        when(passwordVerifier.matches("WrongPassword!", "hash")).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("incorrect");
    }
    
    @Test
    void login_UserNotFound() {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword!");

        when(userRepository.findByNormalizedEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordVerifier.matches("WrongPassword!", null)).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("incorrect");
    }
}
