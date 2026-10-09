package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.dto.request.PasswordChangeRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.PasswordChangeResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.PasswordMapper;
import com.wildlife.wildlife_conservationbackend.service.PasswordService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = EndPoint.BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PasswordController {
    private final PasswordService passwordService;
    private final PasswordMapper mapper;

    @PostMapping(value = EndPoint.CHANGE_PASSWORD, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StandardResponse<PasswordChangeResponseDTO>> change(
            @AuthenticationPrincipal CurrentUser actor, @Valid @RequestBody PasswordChangeRequestDTO request) {
        return passwordService.changePassword(actor, mapper.toRequest(request));
    }
}
