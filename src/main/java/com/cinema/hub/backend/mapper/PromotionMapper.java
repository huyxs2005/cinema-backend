package com.cinema.hub.backend.mapper;

import com.cinema.hub.backend.dto.promotion.PromotionDetailDto;
import com.cinema.hub.backend.dto.promotion.PromotionResponseDto;
import com.cinema.hub.backend.dto.promotion.PromotionSummaryDto;
import com.cinema.hub.backend.entity.Promotion;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PromotionMapper {

    public PromotionResponseDto toResponse(Promotion promotion) {
        return PromotionResponseDto.builder()
                .id(promotion.getId())
                .slug(promotion.getSlug())
                .title(promotion.getTitle())
                .thumbnailUrl(promotion.getThumbnailUrl())
                .content(promotion.getContent())
                .imgContentUrl(promotion.getImgContentUrl())
                .publishedDate(promotion.getPublishedDate())
                .active(promotion.getIsActive())
                .createdAt(promotion.getCreatedAt())
                .updatedAt(promotion.getUpdatedAt())
                .build();
    }

    public PromotionSummaryDto toSummary(Promotion promotion) {
        return PromotionSummaryDto.builder()
                .id(promotion.getId())
                .slug(promotion.getSlug())
                .title(promotion.getTitle())
                .thumbnailUrl(promotion.getThumbnailUrl())
                .content(promotion.getContent())
                .publishedDate(promotion.getPublishedDate())
                .build();
    }

    public PromotionDetailDto toDetail(Promotion promotion) {
        return PromotionDetailDto.builder()
                .slug(promotion.getSlug())
                .title(promotion.getTitle())
                .thumbnailUrl(promotion.getThumbnailUrl())
                .content(promotion.getContent())
                .imgContentUrl(promotion.getImgContentUrl())
                .publishedDate(promotion.getPublishedDate())
                .build();
    }
}
