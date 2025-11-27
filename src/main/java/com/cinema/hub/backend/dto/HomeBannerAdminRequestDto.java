package com.cinema.hub.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class HomeBannerAdminRequestDto {

    @NotBlank
    @Size(max = 500)
    private String imagePath;

    @NotBlank
    @Size(max = 20)
    private String linkType;

    private Integer movieId;

    private Long promotionId;

    @Size(max = 500)
    private String targetUrl;

    @NotNull
    @Min(1)
    private Integer sortOrder;

    @NotNull
    private Boolean isActive;

    private LocalDate startDate;

    private LocalDate endDate;
}
