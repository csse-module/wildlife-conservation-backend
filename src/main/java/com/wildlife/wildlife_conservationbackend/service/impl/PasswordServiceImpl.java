package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.PasswordChangeRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.PasswordChangeResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.repository.UserPasswordRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.service.PasswordService;
import com.wildlife.wildlife_conservationbackend.utility.PasswordVerifier;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordServiceImpl implements PasswordService {
    private final UserRepository userRepository;
    private final UserPasswordRepository userPasswordRepository;
    private final PasswordVerifier passwordVerifier;
    private final PasswordEncoder passwordEncoder;
    private final ResponseGenerator responseGenerator;

    @Override
    public ResponseEntity<StandardResponse<PasswordChangeResponseDTO>> changePassword(
            CurrentUser actor, PasswordChangeRequest request) {
        UserEntity user = userRepository.findById(actor.getId()).filter(UserEntity::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "A valid access token is required."));
        requireCurrentPassword(user, request);
        UserEntity changed = userPasswordRepository.changePassword(user, passwordEncoder.encode(request.getNewPassword()));
        if (changed == null) {
            throw ApiException.conflict("PASSWORD_CHANGE_CONFLICT", "The account changed. Log in again before retrying.");
        }
        log.info("Password changed userId={}", actor.getId());
        return responseGenerator.generateSuccessResponse(new PasswordChangeResponseDTO(false, true), HttpStatus.OK);
    }

    private void requireCurrentPassword(UserEntity user, PasswordChangeRequest request) {
        if (!passwordVerifier.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "The current password is incorrect.");
        }
        if (passwordVerifier.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw ApiException.invalid("The new password must differ from the current password.");
        }
    }
}
