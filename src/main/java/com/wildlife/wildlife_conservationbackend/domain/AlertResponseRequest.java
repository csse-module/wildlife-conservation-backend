package com.wildlife.wildlife_conservationbackend.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseRequest {
    private String actionTaken;

    private String result;

    private String notes;

    private List<String> photoIds;
}
