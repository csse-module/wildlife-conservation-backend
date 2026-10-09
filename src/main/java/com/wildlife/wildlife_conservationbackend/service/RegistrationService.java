package com.wildlife.wildlife_conservationbackend.service;

import com.wildlife.wildlife_conservationbackend.domain.RegistrationRequest;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.RegistrationParkResponseDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface RegistrationService {
    ResponseEntity<StandardResponse<UserProfileResponseDTO>> register(RegistrationRequest request);
    ResponseEntity<StandardResponse<List<RegistrationParkResponseDTO>>> availableParks();
}
