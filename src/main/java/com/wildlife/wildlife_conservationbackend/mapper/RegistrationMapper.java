package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.RegistrationRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.RegistrationRequestDTO;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class RegistrationMapper {
    public RegistrationRequest toRequest(RegistrationRequestDTO dto) {
        return new RegistrationRequest(dto.getName().strip(), dto.getEmail().toLowerCase(Locale.ROOT),
                dto.getPassword(), dto.getParkId());
    }
}
