package com.wildlife.wildlife_conservationbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class LoginResponseDTO {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UserProfileResponseDTO user;
}
