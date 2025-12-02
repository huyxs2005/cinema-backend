package com.cinema.hub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeBannerResponseDto {

    private int id;

    private String imagePath;

    private String linkType;

    private Integer movieId;
    private String movieTitle;

    private Long promotionId;
    private String promotionSlug;
    private String promotionTitle;

    private String targetUrl;

    private Integer sortOrder;

    private Boolean isActive;

    private LocalDate startDate;

    private LocalDate endDate;
}
