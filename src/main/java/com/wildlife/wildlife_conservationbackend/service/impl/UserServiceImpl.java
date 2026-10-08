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
import com.wildlife.wildlife_conservationbackend.service.UserService;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final MongoPageReader pageReader;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final ParkService parkService;
    private final ResponseGenerator responseGenerator;
    private final AccountCreator accountCreator;

    @Override
    public ResponseEntity<StandardResponse<UserProfileResponseDTO>> createStaff(CurrentUser actor, StaffCreateRequest request) {
        for (String parkId : request.getParkIds()) {
            parkService.requireExistingPark(actor, parkId);
        }
        UserEntity user = accountCreator.create(request.getName(), request.getEmail(), request.getTemporaryPassword(),
                request.getRole(), request.getParkIds(), true);
        log.info("Staff account created actorId={} userId={} role={}", actor.getId(), user.getId(), user.getRole());
        return responseGenerator.generateSuccessResponse(userMapper.toProfile(user), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<StandardResponse<UserProfileResponseDTO>> getProfile(CurrentUser actor) {
        UserEntity user = userRepository.findById(actor.getId()).orElseThrow(() -> ApiException.notFound("User"));
        UserProfileResponseDTO response = userMapper.toProfile(user);

        log.debug("Loaded user profile actorId={}", actor.getId());
        return responseGenerator.generateSuccessResponse(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<PageResponse<UserSummaryResponseDTO>>> listUsers(
            CurrentUser actor, String parkId, Role role, PageQuery page) {
        Role selectedRole = role == null ? Role.RANGER : role;
        Criteria scope = Criteria.where("parkIds").in(parkService.accessibleParkIds(actor, parkId))
                .and("active").is(true).and("role").is(selectedRole);
        PageRequest pageable = page.pageable(Sort.by("name", "id"));
        Page<UserEntity> users = pageReader.find(scope, pageable, UserEntity.class);
        PageResponse<UserSummaryResponseDTO> response = PageResponse.from(users.map(userMapper::toSummary));

        log.debug("Listed user records actorId={} count={}", actor.getId(), users.getNumberOfElements());
        return responseGenerator.generateSuccessResponse(response, HttpStatus.OK);
    }
}
