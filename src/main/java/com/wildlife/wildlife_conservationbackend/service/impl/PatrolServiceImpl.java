package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.AssignmentView;
import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.PatrolAssignmentRequest;
import com.wildlife.wildlife_conservationbackend.domain.PatrolRequest;
import com.wildlife.wildlife_conservationbackend.domain.SaveResult;
import com.wildlife.wildlife_conservationbackend.dto.request.AssignmentQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PatrolQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolAssignmentResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolRouteResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PatrolSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.PatrolAssignmentEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolRouteEntity;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.PatrolAssignmentMapper;
import com.wildlife.wildlife_conservationbackend.mapper.PatrolMapper;
import com.wildlife.wildlife_conservationbackend.mapper.PatrolRouteMapper;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.PatrolAssignmentQueryRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolAssignmentRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolQueryRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolRouteRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
import com.wildlife.wildlife_conservationbackend.service.PatrolService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import com.wildlife.wildlife_conservationbackend.utility.PatrolDateFilter;
import com.wildlife.wildlife_conservationbackend.utility.PatrolDistanceCalculator;
import com.wildlife.wildlife_conservationbackend.utility.PatrolValidator;
import com.wildlife.wildlife_conservationbackend.utility.RequestFingerprint;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
public class PatrolServiceImpl implements PatrolService {
    private final PatrolRepository patrolRepository;
    private final PatrolAssignmentRepository assignmentRepository;
    private final PatrolRouteRepository routeRepository;
    private final UserRepository userRepository;
    private final PatrolAssignmentQueryRepository assignmentQueryRepository;
    private final PatrolQueryRepository patrolQueryRepository;
    private final MongoPageReader pageReader;
    private final ParkService parkService;
    private final PatrolMapper patrolMapper;
    private final PatrolAssignmentMapper assignmentMapper;
    private final PatrolRouteMapper routeMapper;
    private final PatrolValidator patrolValidator;
    private final PatrolDistanceCalculator distanceCalculator;
    private final PatrolDateFilter patrolDateFilter;
    private final RequestFingerprint requestFingerprint;
    private final Clock clock;
    private final ResponseGenerator responseGenerator;

    @Override
    public ResponseEntity<StandardResponse<PatrolRouteResponseDTO>> createRoute(CurrentUser actor, com.wildlife.wildlife_conservationbackend.dto.request.PatrolRouteRequestDTO request) {
        String parkId = request.getParkId();
        if (parkId == null || parkId.trim().isEmpty()) {
            if (!actor.getParkIds().isEmpty()) {
                parkId = actor.getParkIds().iterator().next();
            } else {
                throw ApiException.invalid("User has no associated parks to assign the route.");
            }
        } else {
            parkService.requirePark(actor, parkId);
        }

        PatrolRouteEntity route = new PatrolRouteEntity(
                java.util.UUID.randomUUID().toString(),
                parkId,
                request.getAreaId(),
                request.getName(),
                request.getPlannedDistanceMeters(),
                request.getPathPoints()
        );
        PatrolRouteEntity saved = routeRepository.save(route);
        log.info("Created new patrol route id={} by actorId={}", saved.getId(), actor.getId());
        return responseGenerator.generateSuccessResponse(routeMapper.toResponse(saved), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<StandardResponse<PageResponse<PatrolRouteResponseDTO>>> listRoutes(
            CurrentUser actor, String parkId, PageQuery page) {
        Criteria scope = Criteria.where("parkId").in(parkService.accessibleParkIds(actor, parkId));
        PageRequest pageable = page.pageable(Sort.by("name", "id"));
        Page<PatrolRouteEntity> routes = pageReader.find(scope, pageable, PatrolRouteEntity.class);
        PageResponse<PatrolRouteResponseDTO> response = PageResponse.from(routes.map(routeMapper::toResponse));

        log.debug("Listed patrol routes actorId={} count={}", actor.getId(), routes.getNumberOfElements());
        return responseGenerator.generateSuccessResponse(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<PatrolRouteResponseDTO>> getRoute(CurrentUser actor, String id) {
        PatrolRouteEntity route = findRoute(id);
        parkService.requirePark(actor, route.getParkId());
        PatrolRouteResponseDTO response = routeMapper.toResponse(route);

        log.debug("Loaded patrol route routeId={} actorId={}", id, actor.getId());
        return responseGenerator.generateSuccessResponse(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<PatrolAssignmentResponseDTO>> assign(
            CurrentUser actor, String id, PatrolAssignmentRequest request) {
        log.debug("Processing patrol assignment assignmentId={} managerId={}", id, actor.getId());
        validateAssignmentPeriod(request);
        String requestHash = requestFingerprint.of(request);
        Optional<PatrolAssignmentEntity> existingAssignment = assignmentRepository.findById(id);
        if (existingAssignment.isPresent()) {
            SaveResult<PatrolAssignmentResponseDTO> retry = resolveAssignmentRetry(actor, existingAssignment.get(), requestHash);
            return assignmentResponse(id, retry);
        }

        PatrolRouteEntity route = findRoute(request.getRouteId());
        parkService.requirePark(actor, route.getParkId());
        UserEntity ranger = findEligibleRanger(request.getRangerId(), route.getParkId());
        PatrolAssignmentEntity assignment = new PatrolAssignmentEntity(id, route.getParkId(), route.getId(), ranger.getId(),
                request.getScheduledStartAt(), request.getScheduledEndAt(), actor.getId(), clock.instant(), requestHash);
        SaveResult<PatrolAssignmentResponseDTO> result = saveAssignment(actor, assignment);

        return assignmentResponse(id, result);
    }

    @Override
    public ResponseEntity<StandardResponse<PageResponse<PatrolAssignmentResponseDTO>>> listAssignments(
            CurrentUser actor, AssignmentQuery query, PageQuery page) {
        Criteria scope = buildPatrolScope(actor, query.getParkId());
        PageRequest pageable = page.pageable(Sort.by(Sort.Direction.DESC, "scheduledStartAt", "_id"));
        Page<AssignmentView> assignments = assignmentQueryRepository.find(scope, query.getStatus(), pageable);
        PageResponse<PatrolAssignmentResponseDTO> response = PageResponse.from(
                assignments.map(view -> assignmentMapper.toResponse(view.getAssignment(), view.isCompleted())));

        log.debug("Listed patrol assignments actorId={} count={}", actor.getId(), assignments.getNumberOfElements());
        return responseGenerator.generateSuccessResponse(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<PatrolResponseDTO>> complete(CurrentUser actor, String id, PatrolRequest request) {
        log.debug("Processing patrol completion patrolId={} rangerId={}", id, actor.getId());
        patrolValidator.validate(request);
        String requestHash = requestFingerprint.of(request);
        Optional<PatrolEntity> existingPatrol = patrolRepository.findById(id);
        if (existingPatrol.isPresent()) {
            SaveResult<PatrolResponseDTO> retry = resolvePatrolRetry(actor, existingPatrol.get(), requestHash);
            return patrolResponse(id, retry);
        }

        PatrolAssignmentEntity assignment = findAssignment(request.getAssignmentId());
        requirePatrolAccess(actor, assignment.getParkId(), assignment.getRangerId());
        double recordedDistance = distanceCalculator.calculate(request.getTrackPoints());
        PatrolEntity patrol = new PatrolEntity(id, assignment.getId(), assignment.getRouteId(), assignment.getParkId(), actor.getId(),
                request.getEndedAt(), request, recordedDistance, clock.instant(), requestHash);
        SaveResult<PatrolResponseDTO> result = savePatrol(actor, patrol);

        return patrolResponse(id, result);
    }

    @Override
    public ResponseEntity<StandardResponse<PageResponse<PatrolSummaryResponseDTO>>> listPatrols(
            CurrentUser actor, PatrolQuery query, PageQuery page) {
        Criteria scope = buildPatrolScope(actor, query.getParkId());
        if (query.getRouteId() != null) {
            scope.and("routeId").is(query.getRouteId());
        }
        patrolDateFilter.apply(scope, query);
        PageRequest pageable = page.pageable(Sort.by(Sort.Direction.DESC, "endedAt", "_id"));
        Page<PatrolSummaryResponseDTO> patrols = patrolQueryRepository.find(scope, pageable);
        PageResponse<PatrolSummaryResponseDTO> response = PageResponse.from(patrols);

        log.debug("Listed patrols actorId={} count={}", actor.getId(), patrols.getNumberOfElements());
        return responseGenerator.generateSuccessResponse(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<PatrolResponseDTO>> getPatrol(CurrentUser actor, String id) {
        PatrolEntity patrol = patrolRepository.findById(id).orElseThrow(() -> ApiException.notFound("Patrol"));
        requirePatrolAccess(actor, patrol.getParkId(), patrol.getRangerId());
        PatrolResponseDTO response = patrolMapper.toResponse(patrol);

        log.debug("Loaded patrol patrolId={} actorId={}", id, actor.getId());
        return responseGenerator.generateSuccessResponse(response, HttpStatus.OK);
    }

    private void validateAssignmentPeriod(PatrolAssignmentRequest request) {
        if (!request.getScheduledEndAt().isAfter(request.getScheduledStartAt())) {
            throw ApiException.invalid("scheduledEndAt must be after scheduledStartAt.");
        }
    }

    private PatrolRouteEntity findRoute(String id) {
        return routeRepository.findById(id).orElseThrow(() -> ApiException.notFound("Patrol route"));
    }

    private PatrolAssignmentEntity findAssignment(String id) {
        return assignmentRepository.findById(id).orElseThrow(() -> ApiException.notFound("Patrol assignment"));
    }

    private UserEntity findEligibleRanger(String rangerId, String parkId) {
        UserEntity ranger = userRepository.findById(rangerId).orElseThrow(() -> ApiException.notFound("Ranger"));
        if (!ranger.isActive() || ranger.getRole() != Role.RANGER) {
            throw ApiException.invalid("The selected user must be an active ranger.");
        }
        if (!ranger.getParkIds().isEmpty() && !ranger.getParkIds().contains(parkId)) {
            throw ApiException.invalid("The selected user must be an active ranger assigned to this park.");
        }
        if (ranger.getParkIds().isEmpty()) {
            ranger.getParkIds().add(parkId);
            userRepository.save(ranger);
        }
        return ranger;
    }

    private SaveResult<PatrolAssignmentResponseDTO> saveAssignment(CurrentUser actor, PatrolAssignmentEntity assignment) {
        try {
            PatrolAssignmentEntity saved = assignmentRepository.insert(assignment);
            PatrolAssignmentResponseDTO response = assignmentMapper.toResponse(saved, false);
            log.info("Patrol assigned assignmentId={} rangerId={} managerId={}", saved.getId(), saved.getRangerId(), actor.getId());
            return new SaveResult<>(response, true);
        } catch (DuplicateKeyException exception) {
            Optional<PatrolAssignmentEntity> concurrentAssignment = assignmentRepository.findById(assignment.getId());
            if (concurrentAssignment.isPresent()) {
                return resolveAssignmentRetry(actor, concurrentAssignment.get(), assignment.getRequestHash());
            }
            throw ApiException.conflict("ASSIGNMENT_SCHEDULE_CONFLICT", "This ranger already has an assignment at this start time.");
        }
    }

    private SaveResult<PatrolResponseDTO> savePatrol(CurrentUser actor, PatrolEntity patrol) {
        try {
            PatrolEntity saved = patrolRepository.insert(patrol);
            PatrolResponseDTO response = patrolMapper.toResponse(saved);
            log.info("Patrol completed patrolId={} assignmentId={} rangerId={}", saved.getId(), saved.getAssignmentId(), actor.getId());
            return new SaveResult<>(response, true);
        } catch (DuplicateKeyException exception) {
            Optional<PatrolEntity> concurrentPatrol = patrolRepository.findById(patrol.getId());
            if (concurrentPatrol.isPresent()) {
                return resolvePatrolRetry(actor, concurrentPatrol.get(), patrol.getRequestHash());
            }
            throw ApiException.conflict("ASSIGNMENT_ALREADY_USED", "This assignment already has a completed patrol.");
        }
    }

    private SaveResult<PatrolResponseDTO> resolvePatrolRetry(CurrentUser actor, PatrolEntity existing, String hash) {
        requirePatrolAccess(actor, existing.getParkId(), existing.getRangerId());
        if (!existing.getRangerId().equals(actor.getId()) || !existing.getRequestHash().equals(hash)) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "This patrol ID has already been used with different data.");
        }
        log.debug("Patrol retry accepted patrolId={} rangerId={}", existing.getId(), actor.getId());
        return new SaveResult<>(patrolMapper.toResponse(existing), false);
    }

    private SaveResult<PatrolAssignmentResponseDTO> resolveAssignmentRetry(
            CurrentUser actor, PatrolAssignmentEntity existing, String hash) {
        parkService.requirePark(actor, existing.getParkId());
        if (!existing.getAssignedBy().equals(actor.getId()) || !existing.getRequestHash().equals(hash)) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "This assignment ID has already been used with different data.");
        }
        log.debug("Assignment retry accepted assignmentId={} managerId={}", existing.getId(), actor.getId());
        return new SaveResult<>(assignmentMapper.toResponse(existing, patrolRepository.existsByAssignmentId(existing.getId())), false);
    }

    private ResponseEntity<StandardResponse<PatrolAssignmentResponseDTO>> assignmentResponse(
            String id, SaveResult<PatrolAssignmentResponseDTO> result) {
        URI resourceUri = URI.create(EndPoint.BASE + EndPoint.ASSIGNMENTS + "/" + id);
        return responseGenerator.generateSuccessResponse(result, resourceUri);
    }

    private ResponseEntity<StandardResponse<PatrolResponseDTO>> patrolResponse(String id, SaveResult<PatrolResponseDTO> result) {
        URI resourceUri = URI.create(EndPoint.BASE + EndPoint.PATROLS + "/" + id);
        return responseGenerator.generateSuccessResponse(result, resourceUri);
    }

    private Criteria buildPatrolScope(CurrentUser actor, String requestedPark) {
        Criteria scope = Criteria.where("parkId").in(parkService.accessibleParkIds(actor, requestedPark));
        if (actor.getRole() == Role.RANGER) {
            scope.and("rangerId").is(actor.getId());
        }
        return scope;
    }

    private void requirePatrolAccess(CurrentUser actor, String parkId, String rangerId) {
        parkService.requirePark(actor, parkId);
        if (actor.getRole() == Role.RANGER && !actor.getId().equals(rangerId)) {
            throw ApiException.notFound("Record");
        }
    }
}
