package com.wildlife.wildlife_conservationbackend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PasswordChangeRequest {
    private String currentPassword;
    private String newPassword;
}
