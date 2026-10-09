package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.ReportRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.ReportRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.ReportDetailResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.ReportSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.ReportEntity;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportMapper {
    private final AnalyticsMapper analyticsMapper;

    public ReportRequest toRequest(ReportRequestDTO dto) {
        return new ReportRequest(dto.getParkId(), dto.getReportType(), dto.getFrom(), dto.getTo(), dto.getSections());
    }

    public ReportSummaryResponseDTO toSummary(ReportEntity entity) {
        return new ReportSummaryResponseDTO(entity.getId(), entity.getParkId(), entity.getReportType(), entity.getFrom(),
                entity.getTo(), entity.getSections(), entity.getGeneratedBy(), entity.getGeneratedAt(), "GENERATED",
                downloadUrl(entity.getId()));
    }

    public ReportDetailResponseDTO toResponse(ReportEntity entity) {
        return new ReportDetailResponseDTO(entity.getId(), entity.getParkId(), entity.getReportType(), entity.getFrom(),
                entity.getTo(), entity.getSections(), entity.getGeneratedBy(), entity.getGeneratedAt(), "GENERATED",
                downloadUrl(entity.getId()), analyticsMapper.toResponse(entity.getSnapshot()));
    }

    private String downloadUrl(String id) {
        return EndPoint.BASE + EndPoint.REPORTS + "/" + id + "/download";
    }
}
