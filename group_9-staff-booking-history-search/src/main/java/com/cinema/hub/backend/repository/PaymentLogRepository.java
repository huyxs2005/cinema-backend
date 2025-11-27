package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.PaymentLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Integer> {

    Optional<PaymentLog> findTopByBookingOrderByCreatedAtDesc(Booking booking);
}
