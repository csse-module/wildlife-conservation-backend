package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.ParkResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import java.util.Set;
import org.springframework.http.ResponseEntity;

public interface ParkService {
    ResponseEntity<StandardResponse<PageResponse<ParkResponseDTO>>> listParks(CurrentUser actor, PageQuery page);
    Set<String> accessibleParkIds(CurrentUser actor, String requestedPark);
    void requirePark(CurrentUser actor, String parkId);
    void requireExistingPark(CurrentUser actor, String parkId);
    void requireArea(CurrentUser actor, String parkId, String areaId);
}
