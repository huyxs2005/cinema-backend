package com.cinema.hub.backend.dto.snack;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SnackResponseDto {

    Long id;
    String name;
    String category;
    String description;
    BigDecimal price;
    String imageUrl;
    String servingSize;
    Integer displayOrder;
    Boolean available;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
