package com.wildlife.wildlife_conservationbackend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ParkCreateRequestDTO {
    @NotBlank
    @Size(max = 100)
    private String id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String timezone;

    @Valid
    @NotEmpty
    @Size(max = 50)
    private List<ParkAreaRequestDTO> areas;
}
