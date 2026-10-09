package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.IncidentRequest;
import com.wildlife.wildlife_conservationbackend.domain.SaveResult;
import com.wildlife.wildlife_conservationbackend.dto.request.IncidentQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.IncidentResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.IncidentEntity;
import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.IncidentMapper;
import com.wildlife.wildlife_conservationbackend.repository.IncidentRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.PatrolAssignmentRepository;
import com.wildlife.wildlife_conservationbackend.service.IncidentService;
import com.wildlife.wildlife_conservationbackend.service.MediaService;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
import com.wildlife.wildlife_conservationbackend.utility.DateRangeResolver;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import com.wildlife.wildlife_conservationbackend.utility.ObservationValidator;
import com.wildlife.wildlife_conservationbackend.utility.RequestFingerprint;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.net.URI;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentServiceImpl implements IncidentService {
    private final IncidentRepository repository;
    private final IncidentMapper mapper;
    private final ParkService parkService;
    private final MediaService mediaService;
    private final MongoPageReader pageReader;
    private final DateRangeResolver dateRangeResolver;
    private final ObservationValidator observationValidator;
    private final RequestFingerprint requestFingerprint;
    private final ResponseGenerator responseGenerator;
    private final Clock clock;
    private final PatrolAssignmentRepository assignmentRepository;

    @Override
    public ResponseEntity<StandardResponse<IncidentResponseDTO>> submit(CurrentUser actor, String id, IncidentRequest request) {
        log.debug("Processing Incident submission resourceId={} actorId={}", id, actor.getId());
        String hash = requestFingerprint.of(request);
        var existing = repository.findById(id);
        if (existing.isPresent()) {
            return submissionResponse(id, retry(actor, existing.get(), hash));
        }
        validateReferences(actor, request);
        var entity = IncidentEntity.builder().id(id)
                .parkId(request.getParkId())
                .areaId(request.getAreaId())
                .assignmentId(request.getAssignmentId())
                .type(request.getType())
                .detectedAt(request.getDetectedAt())
                .location(request.getLocation())
                .description(request.getDescription())
                .photoId(request.getPhotoId())
                .reportedBy(actor.getId()).createdAt(clock.instant()).requestHash(hash).build();
        SaveResult<IncidentResponseDTO> result;
        try {
            var saved = repository.insert(entity);
            result = new SaveResult<>(mapper.toResponse(saved), true);
            log.info("Incident submitted resourceId={} actorId={}", id, actor.getId());
        } catch (DuplicateKeyException exception) {
            var concurrent = repository.findById(id).orElseThrow(() ->
                    ApiException.conflict("RESOURCE_ALREADY_EXISTS", "This resource ID has already been used."));
            result = retry(actor, concurrent, hash);
        }
        return submissionResponse(id, result);
    }

    @Override
    public ResponseEntity<StandardResponse<PageResponse<IncidentResponseDTO>>> list(CurrentUser actor, IncidentQuery query, PageQuery page) {
        Criteria scope = Criteria.where("parkId").in(parkService.accessibleParkIds(actor, query.getParkId()));
        if (actor.getRole() == Role.RANGER) {
            scope.and("reportedBy").is(actor.getId());
        }
        if (query.getType() != null) {
            scope.and("type").is(query.getType());
        }
        if (query.getAreaId() != null) {
            scope.and("areaId").is(query.getAreaId());
        }
        dateRangeResolver.applyOptional(scope, query.getParkId(), query.getFrom(), query.getTo(), "detectedAt");
        var entities = pageReader.find(scope, page.pageable(Sort.by(Sort.Direction.DESC, "detectedAt", "_id")), IncidentEntity.class);
        var result = PageResponse.from(entities.map(mapper::toResponse));
        log.debug("Listed Incident records actorId={} count={}", actor.getId(), entities.getNumberOfElements());
        return responseGenerator.generateSuccessResponse(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<IncidentResponseDTO>> get(CurrentUser actor, String id) {
        var entity = repository.findById(id).orElseThrow(() -> ApiException.notFound("Incident"));
        requireReadAccess(actor, entity);
        return responseGenerator.generateSuccessResponse(mapper.toResponse(entity), HttpStatus.OK);
    }

    private void validateReferences(CurrentUser actor, IncidentRequest request) {
        parkService.requireArea(actor, request.getParkId(), request.getAreaId());
        observationValidator.requirePast(request.getDetectedAt());
        if (request.getAssignmentId() != null) {
            var assignment = assignmentRepository.findById(request.getAssignmentId())
                    .orElseThrow(() -> ApiException.notFound("Patrol assignment"));
            if (!assignment.getRangerId().equals(actor.getId()) || !assignment.getParkId().equals(request.getParkId())) {
                throw ApiException.invalid("The assignment must belong to this ranger and park.");
            }
        }
        mediaService.requireOwnedMedia(actor, request.getPhotoId(), request.getParkId(), MediaCategory.INCIDENT);
    }

    private void requireReadAccess(CurrentUser actor, IncidentEntity entity) {
        parkService.requirePark(actor, entity.getParkId());
        if (actor.getRole() == Role.RANGER && !entity.getReportedBy().equals(actor.getId())) {
            throw ApiException.notFound("Incident");
        }
    }

    private SaveResult<IncidentResponseDTO> retry(CurrentUser actor, IncidentEntity entity, String hash) {
        requireReadAccess(actor, entity);
        if (!entity.getReportedBy().equals(actor.getId()) || !entity.getRequestHash().equals(hash)) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "This resource ID has already been used with different data.");
        }
        return new SaveResult<>(mapper.toResponse(entity), false);
    }

    private ResponseEntity<StandardResponse<IncidentResponseDTO>> submissionResponse(String id, SaveResult<IncidentResponseDTO> result) {
        return responseGenerator.generateSuccessResponse(result, URI.create(EndPoint.BASE + EndPoint.INCIDENTS + "/" + id));
    }
}
