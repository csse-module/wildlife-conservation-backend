package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.LoginRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.LoginRequestDTO;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class LoginMapper {
    public LoginRequest toRequest(LoginRequestDTO dto) {
        return new LoginRequest(dto.getEmail().strip().toLowerCase(Locale.ROOT), dto.getPassword());
    }
}
