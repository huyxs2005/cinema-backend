package com.cinema.hub.backend.dto.staff;

import com.cinema.hub.backend.entity.enums.PaymentStatus;
import java.time.LocalDateTime;
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
public class TicketScanResponse {
    private Integer bookingId;
    private String bookingCode;
    private String movieTitle;
    private String customerName;
    private String customerEmail;
    private String auditorium;
    private LocalDateTime showtimeStart;
    private LocalDateTime showtimeEnd;
    private String showtimeLabel;
    private List<String> seats;
    private PaymentStatus paymentStatus;
    private boolean paid;
    private String checkInStatus;
    private int checkedInCount;
    private int totalSeats;
    private boolean fullyCheckedIn;
    private boolean checkinAllowed;
    private boolean showtimeExpired;
}
