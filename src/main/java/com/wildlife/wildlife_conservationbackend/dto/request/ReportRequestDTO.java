package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.enums.ReportSection;
import com.wildlife.wildlife_conservationbackend.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequestDTO {
    @NotBlank
    @Size(max = 80)
    private String parkId;

    @NotNull
    private ReportType reportType;

    @NotNull
    private LocalDate from;

    @NotNull
    private LocalDate to;

    @NotNull
    @Size(min = 1, max = 5)
    private List<@NotNull ReportSection> sections;
}
