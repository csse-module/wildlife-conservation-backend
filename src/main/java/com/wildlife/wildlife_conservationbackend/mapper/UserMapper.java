package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.domain.StaffCreateRequest;
import com.wildlife.wildlife_conservationbackend.dto.request.StaffCreateRequestDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.UserSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public StaffCreateRequest toRequest(StaffCreateRequestDTO dto) {
        return new StaffCreateRequest(dto.getName().strip(), dto.getEmail().toLowerCase(Locale.ROOT),
                dto.getTemporaryPassword(), Role.valueOf(dto.getRole()), Set.copyOf(dto.getParkIds()));
    }

    public UserProfileResponseDTO toProfile(UserEntity user) {
        return new UserProfileResponseDTO(user.getId(), user.getName(), user.getNormalizedEmail(),
                user.getRole(), user.getParkIds(), user.isPasswordChangeRequired());
    }

    public UserSummaryResponseDTO toSummary(UserEntity user) {
        return new UserSummaryResponseDTO(user.getId(), user.getName(), user.getRole());
    }
}
