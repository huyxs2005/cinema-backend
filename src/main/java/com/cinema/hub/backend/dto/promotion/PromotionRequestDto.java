package com.cinema.hub.backend.dto.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRequestDto {

    @NotBlank
    @Size(max = 300)
    private String title;

    @Size(max = 500)
    private String thumbnailUrl;

    @Size(max = 500)
    private String imgContentUrl;

    @NotBlank
    private String content;

    @NotNull
    private LocalDate publishedDate;

    @NotNull
    private Boolean active;
}
