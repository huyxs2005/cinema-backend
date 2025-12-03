package com.cinema.hub.backend.dto.admin.dashboard;

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
public class OrderTableDTO {

    private Integer bookingId;
    private String paymentChannel;
    private String paymentMethod;
    private String paymentProvider;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime paidAt;
    private String description;
    private String accountNumber;
    private String bookingCode;
    private String customerEmail;
    private String customerPhone;
    private Integer userId;
    private Integer createdByStaffId;
    private String paymentStatus;
    private String detail;
    private Integer showtimeId;
}
