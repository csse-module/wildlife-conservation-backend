package com.wildlife.wildlife_conservationbackend.dto.response;

import com.wildlife.wildlife_conservationbackend.entity.ParkArea;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkResponseDTO {
    private String id;
    private String name;
    private String timezone;
    private List<ParkArea> areas;
}
