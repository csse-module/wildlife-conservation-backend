package com.wildlife.wildlife_conservationbackend.utility;

import com.wildlife.wildlife_conservationbackend.domain.DateRange;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DateRangeResolver {
    private final ParkRepository parkRepository;

    public DateRange resolve(String parkId, LocalDate from, LocalDate to) {
        if (parkId == null || from == null || to == null) {
            throw ApiException.invalid("Provide parkId, from and to together.");
        }
        long days = ChronoUnit.DAYS.between(from, to);
        if (days < 0 || days >= 92) {
            throw ApiException.invalid("The date range must be ordered and contain at most 92 days.");
        }
        var park = parkRepository.findById(parkId).orElseThrow(() -> ApiException.notFound("Park"));
        ZoneId zone = ZoneId.of(park.getTimezone());
        return new DateRange(from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant(), park.getTimezone());
    }

    public void applyOptional(Criteria scope, String parkId, LocalDate from, LocalDate to, String field) {
        if (from == null && to == null) {
            return;
        }
        DateRange range = resolve(parkId, from, to);
        scope.and(field).gte(range.getStartInclusive()).lt(range.getEndExclusive());
    }
}
