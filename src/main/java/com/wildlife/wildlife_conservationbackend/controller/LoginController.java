package com.wildlife.wildlife_conservationbackend.controller;

import com.wildlife.wildlife_conservationbackend.dto.request.LoginRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.LoginResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.StandardResponse;
import com.wildlife.wildlife_conservationbackend.mapper.LoginMapper;
import com.wildlife.wildlife_conservationbackend.service.LoginService;
import com.wildlife.wildlife_conservationbackend.utility.EndPoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = EndPoint.BASE, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private final LoginMapper mapper;

    @PostMapping(value = EndPoint.LOGIN, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StandardResponse<LoginResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        return loginService.login(mapper.toRequest(request));
    }
}
