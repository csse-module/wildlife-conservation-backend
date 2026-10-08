package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.LoginRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.LoginResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import org.springframework.http.ResponseEntity;

public interface LoginService {
    ResponseEntity<StandardResponse<LoginResponseDTO>> login(LoginRequest request);
}
