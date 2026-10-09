package com.wildlife.wildlife_conservationbackend.domain;

import com.wildlife.wildlife_conservationbackend.enums.ReportSection;
import com.wildlife.wildlife_conservationbackend.enums.ReportType;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    private String parkId;

    private ReportType reportType;

    private LocalDate from;

    private LocalDate to;

    private List<ReportSection> sections;
}
