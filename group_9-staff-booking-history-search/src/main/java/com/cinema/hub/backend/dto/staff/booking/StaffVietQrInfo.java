package com.cinema.hub.backend.dto.staff.booking;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffVietQrInfo {

    private final String bookingCode;
    private final BigDecimal amount;
    private final String transferContent;
    private final String qrImageUrl;
    private final OffsetDateTime expiresAt;
}
