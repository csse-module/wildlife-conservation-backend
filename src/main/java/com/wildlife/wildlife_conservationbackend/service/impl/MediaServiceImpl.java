package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.MediaUploadRequest;
import com.wildlife.wildlife_conservationbackend.domain.SaveResult;
import com.wildlife.wildlife_conservationbackend.dto.response.MediaResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.MediaEntity;
import com.wildlife.wildlife_conservationbackend.enums.MediaCategory;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.MediaMapper;
import com.wildlife.wildlife_conservationbackend.repository.MediaAccessRepository;
import com.wildlife.wildlife_conservationbackend.repository.MediaRepository;
import com.wildlife.wildlife_conservationbackend.service.MediaService;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import com.wildlife.wildlife_conservationbackend.utility.ImageValidator;
import com.wildlife.wildlife_conservationbackend.utility.MediaStorage;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {
    private final MediaRepository repository;
    private final MediaMapper mapper;
    private final ParkService parkService;
    private final MediaAccessRepository accessRepository;
    private final MediaStorage storage;
    private final ImageValidator imageValidator;
    private final ResponseGenerator responseGenerator;
    private final Clock clock;

    @Override
    public ResponseEntity<StandardResponse<MediaResponseDTO>> upload(
            CurrentUser actor, String id, MediaUploadRequest request, MultipartFile file) {
        parkService.requireExistingPark(actor, request.getParkId());
        requireCategory(actor, request.getCategory());
        byte[] bytes = readUpload(file);
        imageValidator.validate(bytes, file.getContentType());
        String checksum = checksum(bytes);
        var existing = repository.findById(id);
        if (existing.isPresent()) {
            return uploadResponse(id, retry(actor, existing.get(), request, checksum));
        }
        String key = storage.store(checksum, bytes, file.getContentType());
        MediaEntity entity = MediaEntity.builder().id(id).ownerId(actor.getId()).parkId(request.getParkId())
                .category(request.getCategory()).contentType(file.getContentType()).sizeBytes(bytes.length)
                .sha256(checksum).storageKey(key).createdAt(clock.instant()).build();
        SaveResult<MediaResponseDTO> result;
        try {
            result = new SaveResult<>(mapper.toResponse(repository.insert(entity)), true);
            log.info("Image uploaded mediaId={} actorId={} sizeBytes={}", id, actor.getId(), bytes.length);
        } catch (DuplicateKeyException exception) {
            MediaEntity concurrent = repository.findById(id).orElseThrow(() ->
                    ApiException.conflict("RESOURCE_ALREADY_EXISTS", "This media ID has already been used."));
            result = retry(actor, concurrent, request, checksum);
        }
        return uploadResponse(id, result);
    }

    @Override
    public ResponseEntity<byte[]> content(CurrentUser actor, String id) {
        MediaEntity media = find(id);
        parkService.requirePark(actor, media.getParkId());
        if (!media.getOwnerId().equals(actor.getId()) && !accessRepository.isLinkedAndReadable(actor, id)) {
            throw ApiException.notFound("Media");
        }
        byte[] bytes = storage.read(media.getStorageKey());
        if (bytes.length != media.getSizeBytes() || !checksum(bytes).equals(media.getSha256())) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE", "Image storage is temporarily unavailable.");
        }
        String filename = id + ("image/png".equals(media.getContentType()) ? ".png" : ".jpg");
        log.debug("Image content loaded mediaId={} actorId={}", id, actor.getId());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(media.getContentType()))
                .contentLength(bytes.length).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
                .header("X-Content-Type-Options", "nosniff").body(bytes);
    }

    @Override
    public void requireOwnedMedia(CurrentUser actor, String id, String parkId, MediaCategory category) {
        MediaEntity media = find(id);
        if (!media.getOwnerId().equals(actor.getId()) || !media.getParkId().equals(parkId) || media.getCategory() != category) {
            throw ApiException.invalid("The image must belong to this account and park, with the correct category.");
        }
    }

    private MediaEntity find(String id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Media"));
    }

    private void requireCategory(CurrentUser actor, MediaCategory category) {
        Set<MediaCategory> allowed = switch (actor.getRole()) {
            case RANGER -> Set.of(MediaCategory.INCIDENT, MediaCategory.ALERT_RESPONSE);
            case LIAISON_OFFICER -> Set.of(MediaCategory.ALERT_RESPONSE);
            case COMMUNITY_MEMBER -> Set.of(MediaCategory.COMMUNITY_REPORT);
            case RESEARCHER, PARK_MANAGER -> Set.of(MediaCategory.CAMERA_TRAP);
        };
        if (!allowed.contains(category)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEDIA_CATEGORY_DENIED", "Your role cannot upload this image category.");
        }
    }

    private byte[] readUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.invalid("An image file is required.");
        }
        if (file.getSize() > ImageValidator.MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Images must not exceed 5 MiB.");
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "UPLOAD_UNAVAILABLE", "The uploaded image could not be read.");
        }
    }

    private String checksum(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private SaveResult<MediaResponseDTO> retry(CurrentUser actor, MediaEntity existing, MediaUploadRequest request, String checksum) {
        if (!existing.getOwnerId().equals(actor.getId()) || !existing.getParkId().equals(request.getParkId())
                || existing.getCategory() != request.getCategory() || !existing.getSha256().equals(checksum)) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "This media ID has already been used with different data.");
        }
        return new SaveResult<>(mapper.toResponse(existing), false);
    }

    private ResponseEntity<StandardResponse<MediaResponseDTO>> uploadResponse(String id, SaveResult<MediaResponseDTO> result) {
        return responseGenerator.generateSuccessResponse(result, URI.create(EndPoint.BASE + EndPoint.MEDIA + "/" + id + "/content"));
    }
}
