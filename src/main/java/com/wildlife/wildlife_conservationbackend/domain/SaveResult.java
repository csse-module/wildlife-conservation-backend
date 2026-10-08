package com.wildlife.wildlife_conservationbackend.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveResult<T> {
    private T data;
    private boolean created;
}
