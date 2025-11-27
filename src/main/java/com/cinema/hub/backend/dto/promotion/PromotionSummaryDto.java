package com.cinema.hub.backend.dto.promotion;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PromotionSummaryDto {

    private final Long id;
    private final String slug;
    private final String title;
    private final String thumbnailUrl;
    private final String content;
    private final LocalDate publishedDate;
}
