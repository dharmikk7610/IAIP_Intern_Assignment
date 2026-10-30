package com.ems.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable paging envelope (Spring's Page JSON is not a stable API). pageNumber is 1-based. */
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
