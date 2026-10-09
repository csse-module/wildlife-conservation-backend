package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.enums.ReportSection;
import com.wildlife.wildlife_conservationbackend.enums.ReportType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDetailResponseDTO {
    private String id;

    private String parkId;

    private ReportType reportType;

    private LocalDate from;

    private LocalDate to;

    private List<ReportSection> sections;

    private String generatedBy;

    private Instant generatedAt;

    private String status;

    private String downloadUrl;

    private AnalyticsSummaryResponseDTO snapshot;
}
