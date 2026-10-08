package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.config.CommunityRegistrationProperties;
import com.wildlife.wildlife_conservationbackend.domain.RegistrationRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.UserMapper;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.service.RegistrationService;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {
    private final CommunityRegistrationProperties properties;
    private final ParkRepository parkRepository;
    private final AccountCreator accountCreator;
    private final UserMapper userMapper;
    private final ResponseGenerator responseGenerator;

    @Override
    public ResponseEntity<StandardResponse<UserProfileResponseDTO>> register(RegistrationRequest request) {
        requireRegistrationPark(request.getParkId());
        UserEntity user = accountCreator.create(request.getName(), request.getEmail(), request.getPassword(),
                Role.COMMUNITY_MEMBER, Set.of(request.getParkId()), false);
        log.info("Community account created userId={} parkId={}", user.getId(), request.getParkId());
        return responseGenerator.generateSuccessResponse(userMapper.toProfile(user), HttpStatus.CREATED);
    }

    private void requireRegistrationPark(String parkId) {
        if (!properties.getCommunityParkIds().contains(parkId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REGISTRATION_NOT_ALLOWED",
                    "Community registration is not enabled for this park.");
        }
        if (!parkRepository.existsById(parkId)) {
            throw ApiException.invalid("The selected registration park does not exist.");
        }
    }
}
