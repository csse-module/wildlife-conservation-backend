package com.wildlife.wildlife_conservationbackend.dto.request;

import com.wildlife.wildlife_conservationbackend.validation.Utf8Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.CodePointLength;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class PasswordChangeRequestDTO {
    @NotBlank
    @Size(max = 72)
    @Utf8Size(max = 72)
    private String currentPassword;
    @NotBlank
    @CodePointLength(min = 6, max = 72)
    @Utf8Size(max = 72)
    private String newPassword;
}
