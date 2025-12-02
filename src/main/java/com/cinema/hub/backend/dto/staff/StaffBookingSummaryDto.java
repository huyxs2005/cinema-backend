package com.cinema.hub.backend.dto.staff;

import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
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
public class StaffBookingSummaryDto {
    private Integer bookingId;
    private String bookingCode;
    private String movieTitle;
    private String auditoriumName;
    private LocalDateTime showtimeStart;
    private LocalDateTime showtimeEnd;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String createdByStaffName;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private OffsetDateTime paidAt;
    private List<StaffBookingSeatDto> seats;
    private String ticketPdfBase64;
}
