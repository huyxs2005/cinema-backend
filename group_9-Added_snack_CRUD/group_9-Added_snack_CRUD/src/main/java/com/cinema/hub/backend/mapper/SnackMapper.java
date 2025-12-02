package com.cinema.hub.backend.mapper;

import com.cinema.hub.backend.dto.snack.SnackResponseDto;
import com.cinema.hub.backend.entity.Snack;

public final class SnackMapper {

    private SnackMapper() {
    }

    public static SnackResponseDto toResponse(Snack snack) {
        if (snack == null) {
            return null;
        }
        return SnackResponseDto.builder()
                .id(snack.getId())
                .name(snack.getName())
                .category(snack.getCategory())
                .description(snack.getDescription())
                .price(snack.getPrice())
                .imageUrl(snack.getImageUrl())
                .servingSize(snack.getServingSize())
                .displayOrder(snack.getDisplayOrder())
                .available(snack.getAvailable())
                .createdAt(snack.getCreatedAt())
                .updatedAt(snack.getUpdatedAt())
                .build();
    }
}
