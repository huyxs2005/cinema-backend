package com.cinema.hub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "PaymentLogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentLogId")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingId", nullable = false)
    private Booking booking;

    @Column(name = "Provider", nullable = false)
    private String provider;

    @Column(name = "Amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "ProviderTransactionId")
    private String providerTransactionId;

    @Column(name = "Status", nullable = false)
    private String status;

    @Column(name = "RawMessage", columnDefinition = "TEXT")
    private String rawMessage;

    @Column(name = "CreatedAt", nullable = false)
    private OffsetDateTime createdAt;
}
