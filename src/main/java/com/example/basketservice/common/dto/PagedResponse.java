package com.example.basketservice.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable, explicit pagination envelope.
 *
 * <p>We deliberately avoid serialising Spring Data's {@code Page} directly: its
 * JSON shape is considered an implementation detail and has changed between
 * Spring versions, which would silently break API consumers.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
