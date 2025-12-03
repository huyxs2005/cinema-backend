package com.cinema.hub.backend.dto.admin.analytics;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayOSTransactionDto {

    private final String bookingCode;
    private final String provider;
    private final BigDecimal amount;
    private final String status;
    private final OffsetDateTime createdAt;
    private final String payosOrderId;
    private final String rawPayloadShort;
}
