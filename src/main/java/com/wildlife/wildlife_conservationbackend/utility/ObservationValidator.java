package com.wildlife.wildlife_conservationbackend.utility;

import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ObservationValidator {
    private final Clock clock;

    public void requirePast(Instant observedAt) {
        if (observedAt.isAfter(clock.instant().plusSeconds(300))) {
            throw ApiException.invalid("An observation cannot be in the future.");
        }
    }
}
