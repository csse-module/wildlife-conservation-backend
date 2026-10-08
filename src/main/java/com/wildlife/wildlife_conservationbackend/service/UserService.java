package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.PageQuery;
import com.wildlife.wildlife_conservationbackend.dto.response.PageResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.UserSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import org.springframework.http.ResponseEntity;

public interface UserService {
    ResponseEntity<StandardResponse<UserProfileResponseDTO>> getProfile(CurrentUser actor);
    ResponseEntity<StandardResponse<PageResponse<UserSummaryResponseDTO>>> listUsers(
            CurrentUser actor, String parkId, Role role, PageQuery page);
}
