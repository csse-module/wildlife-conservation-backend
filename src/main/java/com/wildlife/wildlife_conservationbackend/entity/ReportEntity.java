package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.domain.AnalyticsSnapshot;
import com.wildlife.wildlife_conservationbackend.enums.ReportSection;
import com.wildlife.wildlife_conservationbackend.enums.ReportType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("reports")
@CompoundIndex(name = "park_generatedAt", def = "{'parkId': 1, 'generatedAt': -1, '_id': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportEntity {
    @MongoId
    private String id;

    @Indexed
    private String parkId;

    private ReportType reportType;

    private LocalDate from;

    private LocalDate to;

    private List<ReportSection> sections;

    private String generatedBy;

    private Instant generatedAt;

    private AnalyticsSnapshot snapshot;

    private String requestHash;
}
