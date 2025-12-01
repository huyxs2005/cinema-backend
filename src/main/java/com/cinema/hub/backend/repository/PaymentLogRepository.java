package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, Long> {

    Optional<PaymentLog> findTopByProviderTransactionIdOrderByCreatedAtDesc(String providerTransactionId);
}
