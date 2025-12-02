package com.cinema.hub.backend.dto.snack;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SnackRequestDto {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 50)
    private String category;

    @Size(max = 1000)
    private String description;

    @NotNull
    @DecimalMin(value = "0.01", message = "Gi\u00e1 b\u00e1n ph\u1ea3i l\u1edbn h\u01a1n 0")
    private BigDecimal price;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 50)
    private String servingSize;

    private Boolean available;

    @Min(1)
    @Max(999)
    private Integer displayOrder;
}
