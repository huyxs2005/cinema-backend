package com.cinema.hub.backend.dto.common;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final String sort;

    public static <T> PageResponse<T> from(Page<T> page) {
        Sort sort = page.getPageable().getSort();
        String sortValue = sort.isUnsorted()
                ? null
                : sort.stream()
                        .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                        .collect(Collectors.joining(";"));
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                sortValue);
    }
}
