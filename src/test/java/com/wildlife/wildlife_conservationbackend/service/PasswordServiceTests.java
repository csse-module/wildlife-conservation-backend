package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.PasswordChangeRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.PasswordChangeResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.repository.UserPasswordRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.service.impl.PasswordServiceImpl;
import com.wildlife.wildlife_conservationbackend.utility.PasswordVerifier;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPasswordRepository userPasswordRepository;
    @Mock
    private PasswordVerifier passwordVerifier;
    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordServiceImpl passwordService;
    private ResponseGenerator responseGenerator;

    @BeforeEach
    void setUp() {
        responseGenerator = new ResponseGenerator();
        passwordService = new PasswordServiceImpl(
                userRepository,
                userPasswordRepository,
                passwordVerifier,
                passwordEncoder,
                responseGenerator
        );
    }

    @Test
    void changePassword_Success() {
        CurrentUser actor = new CurrentUser("actor-id", Role.RANGER, Set.of("park1"));
        PasswordChangeRequest request = new PasswordChangeRequest("OldPass1!", "NewPass1!");
        UserEntity user = new UserEntity("actor-id", "User", "u@example.com", "hash", Role.RANGER, Set.of("park1"), true);
        
        when(userRepository.findById("actor-id")).thenReturn(Optional.of(user));
        when(passwordVerifier.matches("OldPass1!", "hash")).thenReturn(true);
        when(passwordVerifier.matches("NewPass1!", "hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPass1!")).thenReturn("new-hash");
        when(userPasswordRepository.changePassword(user, "new-hash")).thenReturn(user);

        ResponseEntity<StandardResponse<PasswordChangeResponseDTO>> response = passwordService.changePassword(actor, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void changePassword_IncorrectCurrentPassword() {
        CurrentUser actor = new CurrentUser("actor-id", Role.RANGER, Set.of("park1"));
        PasswordChangeRequest request = new PasswordChangeRequest("WrongPass1!", "NewPass1!");
        UserEntity user = new UserEntity("actor-id", "User", "u@example.com", "hash", Role.RANGER, Set.of("park1"), true);
        
        when(userRepository.findById("actor-id")).thenReturn(Optional.of(user));
        when(passwordVerifier.matches("WrongPass1!", "hash")).thenReturn(false);

        assertThatThrownBy(() -> passwordService.changePassword(actor, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("current password is incorrect");
    }

    @Test
    void changePassword_NewSameAsOld() {
        CurrentUser actor = new CurrentUser("actor-id", Role.RANGER, Set.of("park1"));
        PasswordChangeRequest request = new PasswordChangeRequest("OldPass1!", "OldPass1!");
        UserEntity user = new UserEntity("actor-id", "User", "u@example.com", "hash", Role.RANGER, Set.of("park1"), true);
        
        when(userRepository.findById("actor-id")).thenReturn(Optional.of(user));
        when(passwordVerifier.matches("OldPass1!", "hash")).thenReturn(true);

        assertThatThrownBy(() -> passwordService.changePassword(actor, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must differ");
    }
}
