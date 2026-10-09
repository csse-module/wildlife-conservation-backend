package com.wildlife.wildlife_conservationbackend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageQuery {
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    @Min(0)
    @Max(100000)
    private Integer page = DEFAULT_PAGE;

    @Min(1)
    @Max(100)
    private Integer size = DEFAULT_SIZE;

    public PageRequest pageable(Sort sort) {
        return PageRequest.of(page == null ? DEFAULT_PAGE : page, size == null ? DEFAULT_SIZE : size, sort);
    }
}
