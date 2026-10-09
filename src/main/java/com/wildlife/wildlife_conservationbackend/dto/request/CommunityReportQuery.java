package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.enums.CommunityReportType;
import com.wildlife.wildlife_conservationbackend.enums.CommunityReportStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityReportQuery {
    @Size(max = 80)
    @Pattern(regexp = ".*\\S.*")
    private String parkId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    private CommunityReportType type;

    private CommunityReportStatus status;

    @Size(max = 80)
    @Pattern(regexp = ".*\\S.*")
    private String areaId;
}
