package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.domain.PasswordChangeRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.PasswordChangeResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface PasswordService {
    ResponseEntity<StandardResponse<PasswordChangeResponseDTO>> changePassword(CurrentUser actor, PasswordChangeRequest request);
}
