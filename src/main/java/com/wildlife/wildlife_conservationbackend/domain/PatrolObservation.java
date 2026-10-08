package com.wildlife.wildlife_conservationbackend.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PatrolObservation {
    private String id;

    private String text;

    private Instant observedAt;

    private Location location;
}
