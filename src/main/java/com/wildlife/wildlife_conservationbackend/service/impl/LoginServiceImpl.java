package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.LoginRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.LoginResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.UserMapper;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.service.LoginService;
import com.wildlife.wildlife_conservationbackend.utility.JwtTokenProvider;
import com.wildlife.wildlife_conservationbackend.utility.PasswordVerifier;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final UserRepository userRepository;
    private final PasswordVerifier passwordVerifier;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final ResponseGenerator responseGenerator;

    @Override
    public ResponseEntity<StandardResponse<LoginResponseDTO>> login(LoginRequest request) {
        log.debug("Processing login request");
        UserEntity user = userRepository.findByNormalizedEmail(request.getEmail()).orElse(null);
        boolean passwordMatches = passwordVerifier.matches(request.getPassword(), user == null ? null : user.getPasswordHash());
        if (user == null || !passwordMatches || !user.isActive()) {
            log.warn("Login rejected reason=invalid_credentials");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect.");
        }
        String accessToken = jwtTokenProvider.issue(user);
        UserProfileResponseDTO profile = userMapper.toProfile(user);
        LoginResponseDTO response = new LoginResponseDTO(accessToken, "Bearer", jwtTokenProvider.expiresIn(), profile);

        log.info("Login successful userId={} role={}", user.getId(), user.getRole());
        return responseGenerator.generateSuccessResponse(response, HttpStatus.OK);
    }
}
