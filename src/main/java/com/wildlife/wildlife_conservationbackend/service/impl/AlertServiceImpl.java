package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.AlertSetupRequest;
import com.wildlife.wildlife_conservationbackend.domain.AlertResponseRequest;
import com.wildlife.wildlife_conservationbackend.domain.DeclineRequest;
import com.wildlife.wildlife_conservationbackend.domain.SupportRequest;
import com.wildlife.wildlife_conservationbackend.domain.SaveResult;
import com.wildlife.wildlife_conservationbackend.dto.request.AlertQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertDeclineResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.AlertSupportResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.AlertEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertDeclineEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertSupportEntity;
import com.wildlife.wildlife_conservationbackend.entity.AlertResponseEntity;
import com.wildlife.wildlife_conservationbackend.enums.AlertStatus;
import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.AlertMapper;
import com.wildlife.wildlife_conservationbackend.repository.AlertRepository;
import com.wildlife.wildlife_conservationbackend.repository.AlertTransitionRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.service.AlertService;
import com.wildlife.wildlife_conservationbackend.service.MediaService;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import com.wildlife.wildlife_conservationbackend.utility.ObservationValidator;
import com.wildlife.wildlife_conservationbackend.utility.RequestFingerprint;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
public class AlertServiceImpl implements AlertService {
    private final AlertRepository repository;
    private final AlertTransitionRepository transitions;
    private final AlertMapper mapper;
    private final ParkService parkService;
    private final MediaService mediaService;
    private final MongoPageReader pageReader;
    private final ObservationValidator observationValidator;
    private final RequestFingerprint requestFingerprint;
    private final ResponseGenerator responseGenerator;
    private final Clock clock;

    @Override
    public ResponseEntity<StandardResponse<AlertResponseDTO>> create(CurrentUser actor, String id, AlertSetupRequest request) {
        String hash = requestFingerprint.of(request);
        var existing = repository.findById(id);
        if (existing.isPresent()) {
            return creationResponse(id, creationRetry(actor, existing.get(), hash));
        }
        parkService.requireArea(actor, request.getParkId(), request.getAreaId());
        observationValidator.requirePast(request.getDetectedAt());
        observationValidator.requirePast(request.getLocationUpdatedAt());
        AlertEntity entity = AlertEntity.builder().id(id).parkId(request.getParkId()).areaId(request.getAreaId())
                .animal(request.getAnimal()).collarId(request.getCollarId()).riskLevel(request.getRiskLevel())
                .location(request.getLocation()).locationUpdatedAt(request.getLocationUpdatedAt()).detectedAt(request.getDetectedAt())
                .status(AlertStatus.NEW).createdBy(actor.getId()).createdAt(clock.instant()).requestHash(hash)
                .declines(List.of()).supportRequests(List.of()).build();
        SaveResult<AlertResponseDTO> result;
        try {
            result = new SaveResult<>(mapper.toResponse(repository.insert(entity)), true);
            log.info("Alert created alertId={} managerId={}", id, actor.getId());
        } catch (DuplicateKeyException exception) {
            result = creationRetry(actor, find(actor, id), hash);
        }
        return creationResponse(id, result);
    }

    @Override
    public ResponseEntity<StandardResponse<PageResponse<AlertResponseDTO>>> list(CurrentUser actor, AlertQuery query, PageQuery page) {
        Criteria scope = Criteria.where("parkId").in(parkService.accessibleParkIds(actor, query.getParkId()));
        if (actor.getRole() != Role.PARK_MANAGER) {
            scope.andOperator(new Criteria().orOperator(
                    Criteria.where("status").is(AlertStatus.NEW).and("declines.officerId").ne(actor.getId()),
                    Criteria.where("status").is(AlertStatus.RESPONDING).and("assignedOfficerId").is(actor.getId()),
                    Criteria.where("status").is(AlertStatus.RESOLVED)));
        }
        if (query.getStatus() != null) {
            scope.and("status").is(query.getStatus());
        }
        var alerts = pageReader.find(scope, page.pageable(Sort.by(Sort.Direction.DESC, "detectedAt", "_id")), AlertEntity.class);
        log.debug("Listed alerts actorId={} count={}", actor.getId(), alerts.getNumberOfElements());
        return responseGenerator.generateSuccessResponse(PageResponse.from(alerts.map(mapper::toResponse)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<AlertResponseDTO>> get(CurrentUser actor, String id) {
        return ok(find(actor, id));
    }

    @Override
    public ResponseEntity<StandardResponse<AlertResponseDTO>> accept(CurrentUser actor, String id) {
        AlertEntity alert = find(actor, id);
        if (isAcceptanceRetry(actor, alert)) {
            return ok(alert);
        }
        if (alert.getStatus() != AlertStatus.NEW || hasDeclined(actor, alert)) {
            throw stateConflict();
        }
        AlertEntity updated = transitions.accept(id, alert.getParkId(), actor.getId(), clock.instant());
        if (updated == null) {
            updated = find(actor, id);
            if (!isAcceptanceRetry(actor, updated)) {
                throw ApiException.conflict("ALERT_ALREADY_ASSIGNED", "This alert is no longer available for acceptance.");
            }
        }
        log.info("Alert accepted alertId={} officerId={}", id, actor.getId());
        return ok(updated);
    }

    @Override
    public ResponseEntity<StandardResponse<AlertDeclineResponseDTO>> decline(CurrentUser actor, String id, DeclineRequest request) {
        AlertEntity alert = find(actor, id);
        var prior = alert.getDeclines().stream().filter(d -> d.getOfficerId().equals(actor.getId())).findFirst();
        if (prior.isPresent()) {
            return declineRetry(prior.get(), request);
        }
        if (alert.getStatus() != AlertStatus.NEW || alert.getDeclines().size() >= 100) {
            throw stateConflict();
        }
        var decline = new AlertDeclineEntity(id, actor.getId(), "NEW", request.getReason(), clock.instant());
        AlertEntity updated = transitions.decline(id, alert.getParkId(), decline);
        if (updated == null) {
            var concurrent = find(actor, id).getDeclines().stream().filter(d -> d.getOfficerId().equals(actor.getId())).findFirst();
            if (concurrent.isEmpty()) {
                throw stateConflict();
            }
            return declineRetry(concurrent.get(), request);
        }
        log.info("Alert declined alertId={} officerId={}", id, actor.getId());
        return responseGenerator.generateSuccessResponse(mapper.toResponse(decline), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<AlertSupportResponseDTO>> support(CurrentUser actor, String id, SupportRequest request) {
        AlertEntity alert = find(actor, id);
        var prior = alert.getSupportRequests().stream().filter(s -> s.getRequestId().equals(request.getRequestId())).findFirst();
        if (prior.isPresent()) {
            return supportRetry(actor, prior.get(), request);
        }
        requireAssignedOfficer(actor, alert);
        if (alert.getSupportRequests().size() >= 20) {
            throw ApiException.conflict("SUPPORT_LIMIT_REACHED", "An alert allows at most 20 support requests.");
        }
        var support = new AlertSupportEntity(request.getRequestId(), id, actor.getId(), request.getReason(), "REQUESTED", clock.instant());
        AlertEntity updated = transitions.support(id, alert.getParkId(), support);
        if (updated == null) {
            var concurrent = find(actor, id).getSupportRequests().stream()
                    .filter(s -> s.getRequestId().equals(request.getRequestId())).findFirst();
            if (concurrent.isEmpty()) {
                throw stateConflict();
            }
            return supportRetry(actor, concurrent.get(), request);
        }
        log.info("Alert support recorded alertId={} requestId={} officerId={}", id, request.getRequestId(), actor.getId());
        return responseGenerator.generateSuccessResponse(mapper.toResponse(support), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<AlertResponseDTO>> resolve(CurrentUser actor, String id, AlertResponseRequest request) {
        AlertEntity alert = find(actor, id);
        String hash = requestFingerprint.of(request);
        if (alert.getStatus() == AlertStatus.RESOLVED) {
            return resolutionRetry(actor, alert, hash);
        }
        requireAssignedOfficer(actor, alert);
        if (Set.copyOf(request.getPhotoIds()).size() != request.getPhotoIds().size()) {
            throw ApiException.invalid("photoIds must not contain duplicates.");
        }
        request.getPhotoIds().forEach(photo -> mediaService.requireOwnedMedia(actor, photo, alert.getParkId(), MediaCategory.ALERT_RESPONSE));
        var response = new AlertResponseEntity(request.getActionTaken(), request.getResult(), request.getNotes(),
                request.getPhotoIds(), actor.getId(), clock.instant());
        AlertEntity updated = transitions.resolve(id, alert.getParkId(), response, hash);
        if (updated == null) {
            return resolutionRetry(actor, find(actor, id), hash);
        }
        log.info("Alert resolved alertId={} officerId={}", id, actor.getId());
        return ok(updated);
    }

    private AlertEntity find(CurrentUser actor, String id) {
        AlertEntity alert = repository.findById(id).orElseThrow(() -> ApiException.notFound("Alert"));
        parkService.requirePark(actor, alert.getParkId());
        return alert;
    }

    private boolean isAcceptanceRetry(CurrentUser actor, AlertEntity alert) {
        return actor.getId().equals(alert.getAssignedOfficerId()) && alert.getStatus() != AlertStatus.NEW;
    }

    private boolean hasDeclined(CurrentUser actor, AlertEntity alert) {
        return alert.getDeclines().stream().anyMatch(d -> d.getOfficerId().equals(actor.getId()));
    }

    private void requireAssignedOfficer(CurrentUser actor, AlertEntity alert) {
        if (alert.getStatus() != AlertStatus.RESPONDING || !actor.getId().equals(alert.getAssignedOfficerId())) {
            throw stateConflict();
        }
    }

    private SaveResult<AlertResponseDTO> creationRetry(CurrentUser actor, AlertEntity alert, String hash) {
        parkService.requirePark(actor, alert.getParkId());
        if (!actor.getId().equals(alert.getCreatedBy()) || !hash.equals(alert.getRequestHash())) {
            throw retryConflict();
        }
        return new SaveResult<>(mapper.toResponse(alert), false);
    }

    private ResponseEntity<StandardResponse<AlertDeclineResponseDTO>> declineRetry(AlertDeclineEntity decline, DeclineRequest request) {
        if (!decline.getReason().equals(request.getReason())) {
            throw retryConflict();
        }
        return responseGenerator.generateSuccessResponse(mapper.toResponse(decline), HttpStatus.OK);
    }

    private ResponseEntity<StandardResponse<AlertSupportResponseDTO>> supportRetry(
            CurrentUser actor, AlertSupportEntity support, SupportRequest request) {
        if (!support.getRequestedBy().equals(actor.getId()) || !support.getReason().equals(request.getReason())) {
            throw retryConflict();
        }
        return responseGenerator.generateSuccessResponse(mapper.toResponse(support), HttpStatus.OK);
    }

    private ResponseEntity<StandardResponse<AlertResponseDTO>> resolutionRetry(CurrentUser actor, AlertEntity alert, String hash) {
        if (alert.getStatus() != AlertStatus.RESOLVED || !actor.getId().equals(alert.getAssignedOfficerId())
                || !Objects.equals(hash, alert.getResponseHash())) {
            throw retryConflict();
        }
        return ok(alert);
    }

    private ResponseEntity<StandardResponse<AlertResponseDTO>> creationResponse(String id, SaveResult<AlertResponseDTO> result) {
        return responseGenerator.generateSuccessResponse(result, URI.create(EndPoint.BASE + EndPoint.ALERTS + "/" + id));
    }

    private ResponseEntity<StandardResponse<AlertResponseDTO>> ok(AlertEntity entity) {
        return responseGenerator.generateSuccessResponse(mapper.toResponse(entity), HttpStatus.OK);
    }

    private ApiException retryConflict() {
        return ApiException.conflict("IDEMPOTENCY_CONFLICT", "This operation has already been recorded with different data.");
    }

    private ApiException stateConflict() {
        return ApiException.conflict("ALERT_STATE_CONFLICT", "This alert is not eligible for the requested operation.");
    }
}
