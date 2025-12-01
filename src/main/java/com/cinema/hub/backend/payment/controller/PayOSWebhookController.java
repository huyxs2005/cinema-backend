package com.cinema.hub.backend.payment.controller;

import com.cinema.hub.backend.payment.payos.PayOSWebhookPayload;
import com.cinema.hub.backend.payment.service.PaymentService;
import com.cinema.hub.backend.payment.util.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/payment")
public class PayOSWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody String payload,
                                                             @RequestHeader(value = "x-client-id", required = false) String clientId,
                                                             @RequestHeader(value = "x-api-key", required = false) String apiKey,
                                                             @RequestHeader(value = "x-checksum", required = false) String checksum) {
        try {
            PayOSWebhookPayload webhookPayload = paymentService.verifyPayOSWebhook(payload, clientId, apiKey, checksum);
            paymentService.completeBooking(webhookPayload, payload);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (PaymentException ex) {
            log.warn("PayOS webhook rejected: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ex.getMessage()
            ));
        } catch (Exception ex) {
            log.error("Unexpected PayOS webhook error", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "WEBHOOK_ERROR"
            ));
        }
    }
}
