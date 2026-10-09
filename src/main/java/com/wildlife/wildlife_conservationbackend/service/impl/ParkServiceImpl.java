package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.ParkResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.ParkMapper;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.util.Set;
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
public class ParkServiceImpl implements ParkService {
    private final MongoPageReader pageReader;
    private final ParkMapper parkMapper;
    private final ResponseGenerator responseGenerator;
    private final ParkRepository parkRepository;
    private final com.wildlife.wildlife_conservationbackend.repository.UserRepository userRepository;

    @Override
    public ResponseEntity<StandardResponse<PageResponse<ParkResponseDTO>>> listParks(CurrentUser actor, PageQuery page) {
        Criteria scope = Criteria.where("_id").in(actor.getParkIds());
        PageRequest pageable = page.pageable(Sort.by("name", "id"));
        Page<ParkEntity> parks = pageReader.find(scope, pageable, ParkEntity.class);
        PageResponse<ParkResponseDTO> response = PageResponse.from(parks.map(parkMapper::toResponse));

        log.debug("Listed park records actorId={} count={}", actor.getId(), parks.getNumberOfElements());
        return responseGenerator.generateSuccessResponse(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<ParkResponseDTO>> createPark(CurrentUser actor, com.wildlife.wildlife_conservationbackend.dto.request.ParkCreateRequestDTO request) {
        if (parkRepository.existsById(request.getId())) {
            throw ApiException.invalid("A park with this ID already exists.");
        }
        ParkEntity entity = parkMapper.toEntity(request);
        parkRepository.insert(entity);
        
        userRepository.findById(actor.getId()).ifPresent(user -> {
            Set<String> newParkIds = new java.util.HashSet<>(user.getParkIds());
            newParkIds.add(entity.getId());
            user.setParkIds(newParkIds);
            userRepository.save(user);
        });

        log.info("Park created id={} by actorId={}", entity.getId(), actor.getId());
        
        return responseGenerator.generateSuccessResponse(parkMapper.toResponse(entity), HttpStatus.CREATED);
    }

    @Override
    public Set<String> accessibleParkIds(CurrentUser actor, String requestedPark) {
        if (requestedPark == null) {
            return actor.getParkIds();
        }
        requirePark(actor, requestedPark);
        return Set.of(requestedPark);
    }

    @Override
    public void requirePark(CurrentUser actor, String parkId) {
        if (!actor.getParkIds().contains(parkId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PARK_ACCESS_DENIED", "You do not have access to this park.");
        }
    }

    @Override
    public void requireArea(CurrentUser actor, String parkId, String areaId) {
        requirePark(actor, parkId);
        ParkEntity park = parkRepository.findById(parkId).orElseThrow(() -> ApiException.notFound("Park"));
        if (park.getAreas().stream().noneMatch(area -> area.getId().equals(areaId))) {
            throw ApiException.invalid("The selected area does not belong to this park.");
        }
    }

    @Override
    public void requireExistingPark(CurrentUser actor, String parkId) {
        requirePark(actor, parkId);
        if (!parkRepository.existsById(parkId)) {
            throw ApiException.notFound("Park");
        }
    }
}
