package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.validation.Utf8Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.CodePointLength;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class StaffCreateRequestDTO {
    @NotBlank
    @Size(max = 100)
    private String name;
    @NotBlank
    @Email
    @Size(max = 254)
    private String email;
    @NotBlank
    @CodePointLength(min = 6, max = 72)
    @Utf8Size(max = 72)
    private String temporaryPassword;
    @NotBlank
    @Pattern(regexp = "RANGER|LIAISON_OFFICER|RESEARCHER", message = "must be RANGER, LIAISON_OFFICER or RESEARCHER")
    private String role;
    @Size(max = 20)
    private Set<@NotBlank @Size(max = 100) String> parkIds;
}
