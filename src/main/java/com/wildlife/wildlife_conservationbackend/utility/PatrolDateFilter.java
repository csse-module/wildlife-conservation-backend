package com.wildlife.wildlife_conservationbackend.utility;

import com.wildlife.wildlife_conservationbackend.dto.request.PatrolQuery;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatrolDateFilter {
    private final ParkRepository parkRepository;

    public void apply(Criteria scope, PatrolQuery query) {
        if (query.getFrom() == null && query.getTo() == null) {
            return;
        }
        if (query.getFrom() == null || query.getTo() == null || query.getParkId() == null) {
            throw ApiException.invalid("Provide parkId, from and to together for date filtering.");
        }
        long days = ChronoUnit.DAYS.between(query.getFrom(), query.getTo());
        if (days < 0 || days >= 92) {
            throw ApiException.invalid("The date range must be ordered and contain at most 92 days.");
        }
        var park = parkRepository.findById(query.getParkId()).orElseThrow(() -> ApiException.notFound("Park"));
        ZoneId zone = ZoneId.of(park.getTimezone());
        scope.and("endedAt").gte(query.getFrom().atStartOfDay(zone).toInstant())
                .lt(query.getTo().plusDays(1).atStartOfDay(zone).toInstant());
    }
}
