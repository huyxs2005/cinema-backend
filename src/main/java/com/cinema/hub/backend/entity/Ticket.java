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

import java.time.OffsetDateTime;

@Entity
@Table(name = "Tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketId")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BookingSeatId", nullable = false)
    private BookingSeat bookingSeat;

    @Column(name = "TicketCode", nullable = false)
    private String ticketCode;

    @Column(name = "QRCodeData", nullable = false)
    private String qrCodeData;

    @Column(name = "IssuedAt", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "CheckedInAt")
    private OffsetDateTime checkedInAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CheckedInByStaffId")
    private UserAccount checkedInByStaff;
}
