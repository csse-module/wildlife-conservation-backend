package com.wildlife.wildlife_conservationbackend.domain;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyCount {
    private LocalDate date;

    private long count;
}
