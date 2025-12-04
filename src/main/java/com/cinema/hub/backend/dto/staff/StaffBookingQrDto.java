package com.cinema.hub.backend.dto.staff;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffBookingQrDto {
    private Integer bookingId;
    private String bookingCode;
    private String qrImageUrl; // Base64 data URL
    private BigDecimal amount;
    private String transferContent;
    private OffsetDateTime expiresAt;
}

