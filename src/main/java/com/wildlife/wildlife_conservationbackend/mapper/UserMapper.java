package com.wildlife.wildlife_conservationbackend.mapper;

import com.wildlife.wildlife_conservationbackend.dto.response.UserProfileResponseDTO;
import com.wildlife.wildlife_conservationbackend.dto.response.UserSummaryResponseDTO;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserProfileResponseDTO toProfile(UserEntity user) {
        return new UserProfileResponseDTO(user.getId(), user.getName(), user.getNormalizedEmail(), user.getRole(), user.getParkIds());
    }

    public UserSummaryResponseDTO toSummary(UserEntity user) {
        return new UserSummaryResponseDTO(user.getId(), user.getName(), user.getRole());
    }
}
