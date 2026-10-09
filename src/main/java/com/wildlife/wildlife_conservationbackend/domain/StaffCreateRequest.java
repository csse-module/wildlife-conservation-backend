package com.wildlife.wildlife_conservationbackend.domain;

import com.wildlife.wildlife_conservationbackend.enums.Role;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class StaffCreateRequest {
    private String name;
    private String email;
    private String temporaryPassword;
    private Role role;
    private Set<String> parkIds;
}
