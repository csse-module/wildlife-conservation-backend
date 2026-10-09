package com.wildlife.wildlife_conservationbackend.domain;

import com.wildlife.wildlife_conservationbackend.enums.IncidentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentTypeCount {
    private IncidentType type;

    private long count;
}
