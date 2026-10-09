package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.PasswordChangeRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.PasswordChangeRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class PasswordMapper {
    public PasswordChangeRequest toRequest(PasswordChangeRequestDTO dto) {
        return new PasswordChangeRequest(dto.getCurrentPassword(), dto.getNewPassword());
    }
}
