package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.ReportRequest;
import com.wildlife.wildlife_conservationbackend.domain.SaveResult;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.ReportDetailResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.ReportSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.entity.ReportEntity;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.mapper.ReportMapper;
import com.wildlife.wildlife_conservationbackend.repository.ReportRepository;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.service.AnalyticsService;
import com.wildlife.wildlife_conservationbackend.service.ReportService;
import com.wildlife.wildlife_conservationbackend.service.ParkService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import com.wildlife.wildlife_conservationbackend.utility.RequestFingerprint;
import com.wildlife.wildlife_conservationbackend.utility.ResponseGenerator;
import com.wildlife.wildlife_conservationbackend.utility.ReportPdfRenderer;
import java.net.URI;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {
    private final ReportRepository repository;
    private final ReportMapper mapper;
    private final AnalyticsService analyticsService;
    private final ParkService parkService;
    private final MongoPageReader pageReader;
    private final RequestFingerprint requestFingerprint;
    private final ResponseGenerator responseGenerator;
    private final ReportPdfRenderer pdfRenderer;

    @Override
    public ResponseEntity<StandardResponse<ReportDetailResponseDTO>> generate(CurrentUser actor, String id, ReportRequest request) {
        String hash = requestFingerprint.of(request);
        var existing = repository.findById(id);
        if (existing.isPresent()) {
            return creationResponse(id, retry(actor, existing.get(), hash));
        }
        if (Set.copyOf(request.getSections()).size() != request.getSections().size()) {
            throw ApiException.invalid("sections must not contain duplicates.");
        }
        var snapshot = analyticsService.snapshot(actor, request.getParkId(), request.getFrom(), request.getTo());
        ReportEntity report = ReportEntity.builder().id(id).parkId(request.getParkId()).reportType(request.getReportType())
                .from(request.getFrom()).to(request.getTo()).sections(request.getSections())
                .generatedBy(actor.getId()).generatedAt(snapshot.getGeneratedAt()).snapshot(snapshot).requestHash(hash).build();
        SaveResult<ReportDetailResponseDTO> result;
        try {
            result = new SaveResult<>(mapper.toResponse(repository.insert(report)), true);
            log.info("Report snapshot generated reportId={} actorId={}", id, actor.getId());
        } catch (DuplicateKeyException exception) {
            result = retry(actor, find(actor, id), hash);
        }
        return creationResponse(id, result);
    }

    @Override
    public ResponseEntity<StandardResponse<PageResponse<ReportSummaryResponseDTO>>> list(CurrentUser actor, String parkId, PageQuery page) {
        parkService.requirePark(actor, parkId);
        var reports = pageReader.find(Criteria.where("parkId").is(parkId),
                page.pageable(Sort.by(Sort.Direction.DESC, "generatedAt", "_id")), ReportEntity.class);
        return responseGenerator.generateSuccessResponse(PageResponse.from(reports.map(mapper::toSummary)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<StandardResponse<ReportDetailResponseDTO>> get(CurrentUser actor, String id) {
        return responseGenerator.generateSuccessResponse(mapper.toResponse(find(actor, id)), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<byte[]> download(CurrentUser actor, String id) {
        byte[] bytes = pdfRenderer.render(find(actor, id));
        log.debug("Report downloaded reportId={} actorId={}", id, actor.getId());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(bytes.length)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("report-" + id + ".pdf").build().toString())
                .header("X-Content-Type-Options", "nosniff").body(bytes);
    }

    private ReportEntity find(CurrentUser actor, String id) {
        ReportEntity report = repository.findById(id).orElseThrow(() -> ApiException.notFound("Report"));
        parkService.requirePark(actor, report.getParkId());
        return report;
    }

    private SaveResult<ReportDetailResponseDTO> retry(CurrentUser actor, ReportEntity report, String hash) {
        parkService.requirePark(actor, report.getParkId());
        if (!actor.getId().equals(report.getGeneratedBy()) || !hash.equals(report.getRequestHash())) {
            throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "This report ID has already been used with different data.");
        }
        return new SaveResult<>(mapper.toResponse(report), false);
    }

    private ResponseEntity<StandardResponse<ReportDetailResponseDTO>> creationResponse(String id, SaveResult<ReportDetailResponseDTO> result) {
        return responseGenerator.generateSuccessResponse(result, URI.create(EndPoint.BASE + EndPoint.REPORTS + "/" + id));
    }
}
