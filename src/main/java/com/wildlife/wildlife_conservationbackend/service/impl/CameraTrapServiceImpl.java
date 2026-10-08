package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.CameraImageRequest;
import com.wildlife.wildlife_conservationbackend.domain.CameraReviewRequest;
import com.wildlife.wildlife_conservationbackend.domain.SaveResult;
import com.wildlife.wildlife_conservationbackend.dto.request.CameraImageQuery;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraImageResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.CameraTrapResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.CameraImageEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraReviewEntity;
import com.wildlife.wildlife_conservationbackend.entity.CameraTrapEntity;
import com.wildlife.wildlife_conservationbackend.enums.CameraImageStatus;
import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.CameraTrapMapper;
import com.wildlife.wildlife_conservationbackend.repository.CameraImageRepository;
import com.wildlife.wildlife_conservationbackend.repository.CameraImageTransitionRepository;
import com.wildlife.wildlife_conservationbackend.repository.CameraTrapRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.service.CameraTrapService;
import com.wildlife.wildlife_conservationbackend.service.MediaService;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
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
public class CameraTrapServiceImpl implements CameraTrapService {
    private final CameraImageRepository repository;
    private final CameraTrapRepository cameraTrapRepository;
    private final CameraImageTransitionRepository transitions;
    private final CameraTrapMapper mapper;
    private final ParkService parkService;
    private final MediaService mediaService;
    private final MongoPageReader pageReader;
    private final ObservationValidator observationValidator;
    private final RequestFingerprint requestFingerprint;
    private final ResponseGenerator responseGenerator;
    private final Clock clock;

    @Override
    public ResponseEntity<StandardResponse<PageResponse<CameraTrapResponseDTO>>> listTraps(CurrentUser actor, String parkId, PageQuery page) {
        parkService.requirePark(actor, parkId);
        var traps = pageReader.find(Criteria.where("parkId").is(parkId), page.pageable(Sort.by("name", "_id")), CameraTrapEntity.class);
        return responseGenerator.generateSuccessResponse(PageResponse.from(traps.map(mapper::toResponse)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<CameraImageResponseDTO>> submit(CurrentUser actor, String id, CameraImageRequest request) {
        String hash = requestFingerprint.of(request);
        var existing = repository.findById(id);
        if (existing.isPresent()) {
            return creationResponse(id, creationRetry(actor, existing.get(), hash));
        }
        CameraTrapEntity camera = cameraTrapRepository.findById(request.getCameraTrapId())
                .orElseThrow(() -> ApiException.notFound("Camera trap"));
        parkService.requireArea(actor, camera.getParkId(), camera.getAreaId());
        observationValidator.requirePast(request.getCapturedAt());
        mediaService.requireOwnedMedia(actor, request.getMediaId(), camera.getParkId(), MediaCategory.CAMERA_TRAP);
        CameraImageEntity image = CameraImageEntity.builder().id(id).cameraTrapId(camera.getId()).parkId(camera.getParkId())
                .mediaId(request.getMediaId()).capturedAt(request.getCapturedAt()).status(CameraImageStatus.PENDING_REVIEW)
                .uploadedBy(actor.getId()).createdAt(clock.instant()).requestHash(hash).build();
        SaveResult<CameraImageResponseDTO> result;
        try {
            result = new SaveResult<>(mapper.toResponse(repository.insert(image)), true);
            log.info("Camera image submitted imageId={} actorId={}", id, actor.getId());
        } catch (DuplicateKeyException exception) {
            result = creationRetry(actor, find(actor, id), hash);
        }
        return creationResponse(id, result);
    }

    @Override
    public ResponseEntity<StandardResponse<PageResponse<CameraImageResponseDTO>>> listImages(CurrentUser actor, CameraImageQuery query, PageQuery page) {
        parkService.requirePark(actor, query.getParkId());
        Criteria scope = Criteria.where("parkId").is(query.getParkId());
        if (query.getCameraTrapId() != null) {
            scope.and("cameraTrapId").is(query.getCameraTrapId());
        }
        if (query.getStatus() != null) {
            scope.and("status").is(query.getStatus());
        }
        var images = pageReader.find(scope, page.pageable(Sort.by(Sort.Direction.DESC, "capturedAt", "_id")), CameraImageEntity.class);
        return responseGenerator.generateSuccessResponse(PageResponse.from(images.map(mapper::toResponse)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<CameraImageResponseDTO>> get(CurrentUser actor, String id) {
        return ok(find(actor, id));
    }

    @Override
    public ResponseEntity<StandardResponse<CameraImageResponseDTO>> review(CurrentUser actor, String id, CameraReviewRequest request) {
        CameraImageEntity image = find(actor, id);
        String hash = requestFingerprint.of(request);
        if (image.getStatus() == CameraImageStatus.REVIEWED) {
            return reviewRetry(actor, image, hash);
        }
        var review = new CameraReviewEntity(request.getSpecies(), request.getPossiblePoacher(), request.getNotes(), actor.getId(), clock.instant());
        CameraImageEntity updated = transitions.review(id, image.getParkId(), review, hash);
        if (updated == null) {
            return reviewRetry(actor, find(actor, id), hash);
        }
        log.info("Camera image reviewed imageId={} reviewerId={}", id, actor.getId());
        return ok(updated);
    }

    private CameraImageEntity find(CurrentUser actor, String id) {
        CameraImageEntity image = repository.findById(id).orElseThrow(() -> ApiException.notFound("Camera image"));
        parkService.requirePark(actor, image.getParkId());
        return image;
    }

    private SaveResult<CameraImageResponseDTO> creationRetry(CurrentUser actor, CameraImageEntity image, String hash) {
        parkService.requirePark(actor, image.getParkId());
        if (!actor.getId().equals(image.getUploadedBy()) || !hash.equals(image.getRequestHash())) {
            throw conflict();
        }
        return new SaveResult<>(mapper.toResponse(image), false);
    }

    private ResponseEntity<StandardResponse<CameraImageResponseDTO>> reviewRetry(CurrentUser actor, CameraImageEntity image, String hash) {
        if (image.getStatus() != CameraImageStatus.REVIEWED || image.getReview() == null
                || !actor.getId().equals(image.getReview().getReviewedBy()) || !Objects.equals(hash, image.getReviewHash())) {
            throw conflict();
        }
        return ok(image);
    }

    private ResponseEntity<StandardResponse<CameraImageResponseDTO>> creationResponse(String id, SaveResult<CameraImageResponseDTO> result) {
        return responseGenerator.generateSuccessResponse(result, URI.create(EndPoint.BASE + EndPoint.CAMERA_IMAGES + "/" + id));
    }

    private ResponseEntity<StandardResponse<CameraImageResponseDTO>> ok(CameraImageEntity image) {
        return responseGenerator.generateSuccessResponse(mapper.toResponse(image), HttpStatus.OK);
    }

    private ApiException conflict() {
        return ApiException.conflict("IDEMPOTENCY_CONFLICT", "This image or review has already been recorded with different data.");
    }
}
