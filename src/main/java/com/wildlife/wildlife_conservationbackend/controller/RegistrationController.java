package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.dto.request.RegistrationRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.RegistrationParkResponseDTO;
import java.util.List;
import com.wildlife.wildlife_conservationbackend.mapper.RegistrationMapper;
import com.wildlife.wildlife_conservationbackend.service.RegistrationService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = EndPoint.BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;
    private final RegistrationMapper mapper;

    @PostMapping(value = EndPoint.REGISTER, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StandardResponse<UserProfileResponseDTO>> register(@Valid @RequestBody RegistrationRequestDTO request) {
        return registrationService.register(mapper.toRequest(request));
    }

    @GetMapping(EndPoint.REGISTRATION_PARKS)
    public ResponseEntity<StandardResponse<List<RegistrationParkResponseDTO>>> availableParks() {
        return registrationService.availableParks();
    }
}
