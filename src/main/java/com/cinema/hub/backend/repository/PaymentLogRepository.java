package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.PaymentLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    Optional<PaymentLog> findTopByProviderTransactionIdOrderByCreatedAtDesc(String providerTransactionId);

    @Query("""
        select pl from PaymentLog pl
        where pl.booking.id = :bookingId
        order by pl.createdAt desc
    """)
    List<PaymentLog> findByBooking_IdOrderByCreatedAtDesc(@Param("bookingId") Integer bookingId);
}
