package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.CommunityReportRequest;
import com.wildlife.wildlife_conservationbackend.domain.CommunityResponseRequest;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportStatus;
import com.wildlife.wildlife_conservationbackend.repository.CommunityReportTransitionRepository;
import com.wildlife.wildlife_conservationbackend.domain.SaveResult;
import com.wildlife.wildlife_conservationbackend.dto.request.CommunityReportQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.CommunityReportResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.CommunityReportEntity;
import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.CommunityReportMapper;
import com.wildlife.wildlife_conservationbackend.repository.CommunityReportRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportType;
import com.wildlife.wildlife_conservationbackend.service.CommunityReportService;
import com.wildlife.wildlife_conservationbackend.service.MediaService;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
import com.wildlife.wildlife_conservationbackend.utility.DateRangeResolver;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import com.wildlife.wildlife_conservationbackend.utility.ObservationValidator;
import com.wildlife.wildlife_conservationbackend.utility.RequestFingerprint;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.net.URI;
import java.time.Clock;
import java.util.Objects;
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
public class CommunityReportServiceImpl implements CommunityReportService {
    private final CommunityReportRepository repository;
    private final CommunityReportMapper mapper;
    private final ParkService parkService;
    private final MediaService mediaService;
    private final MongoPageReader pageReader;
    private final DateRangeResolver dateRangeResolver;
    private final ObservationValidator observationValidator;
    private final RequestFingerprint requestFingerprint;
    private final ResponseGenerator responseGenerator;
    private final Clock clock;
    private final CommunityReportTransitionRepository transitions;

    @Override
    public ResponseEntity<StandardResponse<CommunityReportResponseDTO>> submit(CurrentUser actor, String id, CommunityReportRequest request) {
        log.debug("Processing CommunityReport submission resourceId={} actorId={}", id, actor.getId());
        String hash = requestFingerprint.of(request);
        var existing = repository.findById(id);
        if (existing.isPresent()) {
            return submissionResponse(id, retry(actor, existing.get(), hash));
        }
        validateReferences(actor, request);
        var entity = CommunityReportEntity.builder().id(id)
                .parkId(request.getParkId())
                .areaId(request.getAreaId())
                .type(request.getType())
                .species(request.getSpecies())
                .village(request.getVillage())
                .occurredAt(request.getOccurredAt())
                .location(request.getLocation())
                .description(request.getDescription())
                .cropDetails(request.getCropDetails())
                .photoId(request.getPhotoId())
                .reportedBy(actor.getId()).createdAt(clock.instant()).requestHash(hash).build();
        SaveResult<CommunityReportResponseDTO> result;
        try {
            var saved = repository.insert(entity);
            result = new SaveResult<>(mapper.toResponse(saved), true);
            log.info("CommunityReport submitted resourceId={} actorId={}", id, actor.getId());
        } catch (DuplicateKeyException exception) {
            var concurrent = repository.findById(id).orElseThrow(() ->
                    ApiException.conflict("RESOURCE_ALREADY_EXISTS", "This resource ID has already been used."));
            result = retry(actor, concurrent, hash);
        }
        return submissionResponse(id, result);
    }

    @Override
    public ResponseEntity<StandardResponse<PageResponse<CommunityReportResponseDTO>>> list(CurrentUser actor, CommunityReportQuery query, PageQuery page) {
        Criteria scope = Criteria.where("parkId").in(parkService.accessibleParkIds(actor, query.getParkId()));
        if (actor.getRole() == Role.COMMUNITY_MEMBER) {
            scope.and("reportedBy").is(actor.getId());
        }
        if (query.getType() != null) {
            scope.and("type").is(query.getType());
        }
        if (query.getAreaId() != null) {
            scope.and("areaId").is(query.getAreaId());
        }
        if (query.getStatus() == CommunityReportStatus.SUBMITTED) {
            scope.andOperator(new Criteria().orOperator(Criteria.where("status").is(CommunityReportStatus.SUBMITTED),
                    Criteria.where("status").is(null)));
        } else if (query.getStatus() != null) {
            scope.and("status").is(query.getStatus());
        }
        dateRangeResolver.applyOptional(scope, query.getParkId(), query.getFrom(), query.getTo(), "occurredAt");
        var entities = pageReader.find(scope, page.pageable(Sort.by(Sort.Direction.DESC, "createdAt", "_id")), CommunityReportEntity.class);
        var result = PageResponse.from(entities.map(mapper::toResponse));
        log.debug("Listed CommunityReport records actorId={} count={}", actor.getId(), entities.getNumberOfElements());
        return responseGenerator.generateSuccessResponse(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<CommunityReportResponseDTO>> get(CurrentUser actor, String id) {
        var entity = find(actor, id);
        return responseGenerator.generateSuccessResponse(mapper.toResponse(entity), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<CommunityReportResponseDTO>> accept(CurrentUser actor, String id) {
        CommunityReportEntity existing = find(actor, id);
        if (existing.getStatus() == CommunityReportStatus.RESPONDING && actor.getId().equals(existing.getAssignedOfficerId())) {
            return responseGenerator.generateSuccessResponse(mapper.toResponse(existing), HttpStatus.OK);
        }
        var updated = transitions.accept(id, existing.getParkId(), actor.getId(), clock.instant());
        if (updated == null) {
            existing = find(actor, id);
            if (existing.getStatus() == CommunityReportStatus.RESPONDING && actor.getId().equals(existing.getAssignedOfficerId())) {
                return responseGenerator.generateSuccessResponse(mapper.toResponse(existing), HttpStatus.OK);
            }
            throw ApiException.conflict("COMMUNITY_REPORT_ALREADY_ASSIGNED", "This report has already been accepted or resolved.");
        }
        log.info("Community report accepted reportId={} officerId={}", id, actor.getId());
        return responseGenerator.generateSuccessResponse(mapper.toResponse(updated), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<CommunityReportResponseDTO>> resolve(CurrentUser actor, String id, CommunityResponseRequest request) {
        CommunityReportEntity existing = find(actor, id);
        if (!actor.getId().equals(existing.getAssignedOfficerId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "REPORT_ASSIGNEE_REQUIRED", "Only the officer who accepted this report can resolve it.");
        }
        if (sameResolution(existing, request)) {
            return responseGenerator.generateSuccessResponse(mapper.toResponse(existing), HttpStatus.OK);
        }
        var updated = transitions.resolve(id, existing.getParkId(), actor.getId(), request, clock.instant());
        if (updated == null) {
            existing = find(actor, id);
            if (sameResolution(existing, request)) {
                return responseGenerator.generateSuccessResponse(mapper.toResponse(existing), HttpStatus.OK);
            }
            throw ApiException.conflict("COMMUNITY_RESPONSE_CONFLICT", "This report must be accepted before recording its response, and resolved responses cannot be changed.");
        }
        log.info("Community report resolved reportId={} officerId={}", id, actor.getId());
        return responseGenerator.generateSuccessResponse(mapper.toResponse(updated), HttpStatus.OK);
    }

    private boolean sameResolution(CommunityReportEntity entity, CommunityResponseRequest request) {
        return entity.getStatus() == CommunityReportStatus.RESOLVED
                && Objects.equals(entity.getActionTaken(), request.getActionTaken()) && Objects.equals(entity.getResult(), request.getResult());
    }

    private CommunityReportEntity find(CurrentUser actor, String id) {
        var entity = repository.findById(id).orElseThrow(() -> ApiException.notFound("CommunityReport"));
        requireReadAccess(actor, entity);
        return entity;
    }

    private void validateReferences(CurrentUser actor, CommunityReportRequest request) {
        parkService.requireArea(actor, request.getParkId(), request.getAreaId());
        observationValidator.requirePast(request.getOccurredAt());
        if (request.getType() == CommunityReportType.WILDLIFE_SIGHTING
                && (request.getSpecies() == null || request.getSpecies().isBlank())) {
            throw ApiException.invalid("species is required for a wildlife sighting.");
        }
        if (request.getType() == CommunityReportType.CROP_DAMAGE
                && (request.getCropDetails() == null || request.getCropDetails().isBlank())) {
            throw ApiException.invalid("cropDetails is required for crop damage.");
        }
        if (request.getPhotoId() != null) {
            mediaService.requireOwnedMedia(actor, request.getPhotoId(), request.getParkId(), MediaCategory.COMMUNITY_REPORT);
        }
    }

    private void requireReadAccess(CurrentUser actor, CommunityReportEntity entity) {
        parkService.requirePark(actor, entity.getParkId());
        if (actor.getRole() == Role.COMMUNITY_MEMBER && !entity.getReportedBy().equals(actor.getId())) {
            throw ApiException.notFound("CommunityReport");
        }
    }

    private SaveResult<CommunityReportResponseDTO> retry(CurrentUser actor, CommunityReportEntity entity, String hash) {
        requireReadAccess(actor, entity);
        if (!entity.getReportedBy().equals(actor.getId()) || !entity.getRequestHash().equals(hash)) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "This resource ID has already been used with different data.");
        }
        return new SaveResult<>(mapper.toResponse(entity), false);
    }

    private ResponseEntity<StandardResponse<CommunityReportResponseDTO>> submissionResponse(String id, SaveResult<CommunityReportResponseDTO> result) {
        return responseGenerator.generateSuccessResponse(result, URI.create(EndPoint.BASE + EndPoint.COMMUNITY_REPORTS + "/" + id));
    }
}
