package com.cinema.hub.backend.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public final class PaginationUtil {

    private PaginationUtil() {
    }

    public static Pageable create(int page, int size, String sort) {
        if (!StringUtils.hasText(sort)) {
            return PageRequest.of(page, size);
        }
        String[] parts = sort.split(",");
        String property = parts[0];
        Sort.Direction direction = parts.length > 1
                ? Sort.Direction.fromString(parts[1])
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
