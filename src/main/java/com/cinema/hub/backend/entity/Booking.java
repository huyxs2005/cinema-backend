package com.cinema.hub.backend.entity;

import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "Bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BookingId")
    private Integer id;

    @Column(name = "BookingCode", nullable = false)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserId")
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedByStaffId")
    private UserAccount createdByStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ShowtimeId", nullable = false)
    private Showtime showtime;

    @Column(name = "VoucherId")
    private Integer voucherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "BookingStatus", nullable = false)
    private BookingStatus bookingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "PaymentStatus", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "PaymentMethod")
    private String paymentMethod;

    @Column(name = "TotalAmount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "FinalAmount", nullable = false)
    private BigDecimal finalAmount;

    @Column(name = "CreatedAt", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "PaidAt")
    private OffsetDateTime paidAt;

    @Column(name = "CancelledAt")
    private OffsetDateTime cancelledAt;
}
