package com.cinema.hub.backend.dto.promotion;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PromotionResponseDto {

    private final Long id;
    private final String slug;
    private final String title;
    private final String thumbnailUrl;
    private final String content;
    private final String imgContentUrl;
    private final LocalDate publishedDate;
    private final Boolean active;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}
