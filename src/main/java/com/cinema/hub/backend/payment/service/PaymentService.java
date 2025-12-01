package com.cinema.hub.backend.payment.service;

import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.BookingSeat;
import com.cinema.hub.backend.entity.PaymentLog;
import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.BookingSeatRepository;
import com.cinema.hub.backend.repository.PaymentLogRepository;
import com.cinema.hub.backend.service.BookingService;
import com.cinema.hub.backend.payment.payos.PayOSClient;
import com.cinema.hub.backend.payment.payos.PayOSPaymentData;
import com.cinema.hub.backend.payment.payos.PayOSPaymentRequest;
import com.cinema.hub.backend.payment.payos.PayOSPaymentStatus;
import com.cinema.hub.backend.payment.payos.PayOSWebhookPayload;
import com.cinema.hub.backend.payment.payos.PayOSWebhookValidator;
import com.cinema.hub.backend.payment.model.PaymentCheckoutResponse;
import com.cinema.hub.backend.payment.model.PaymentCreateRequest;
import com.cinema.hub.backend.payment.util.PaymentException;
import com.cinema.hub.backend.payment.vietqr.PaymentQrService;
import com.cinema.hub.backend.util.TimeProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String BANK_NAME = "MB Bank";
    private static final String ACCOUNT_NUMBER = "0931630902";
    private static final String ACCOUNT_NAME = "DAO NAM HAI";

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final PaymentQrService paymentQrService;
    private final TicketEmailService ticketEmailService;
    private final BookingService bookingService;
    private final ObjectMapper objectMapper;
    private final PayOSWebhookValidator webhookValidator;
    private final PayOSClient payOSClient;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    @Transactional
    public PaymentCheckoutResponse createVietQR(PaymentCreateRequest request) {
        Booking booking = bookingRepository.findByIdForUpdate(request.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + request.getBookingId()));
        validateBookingForPayment(booking);

        BigDecimal amount = calculateAmount(booking).setScale(0, RoundingMode.HALF_UP);
        booking.setFinalAmount(amount);
        bookingRepository.save(booking);

        long orderCode = generateOrderCode();
        String transferContent = buildTransferContent(booking, orderCode);
        PayOSPaymentRequest payOSRequest = PayOSPaymentRequest.builder()
                .orderCode(orderCode)
                .amount(amount.longValueExact())
                .description(transferContent)
                .returnUrl(buildReturnUrl(booking))
                .cancelUrl(buildCancelUrl(booking))
                .build();
        PayOSPaymentData paymentData = payOSClient.createPaymentRequest(payOSRequest);

        String qrBase64 = paymentQrService.renderPayload(paymentData.getQrCode());
        OffsetDateTime expiresAt = paymentData.getExpiredAt();
        if (expiresAt == null) {
            expiresAt = TimeProvider.now().plusMinutes(10);
        }

        persistLog(booking, amount, orderCode, transferContent, request.getEmail(), expiresAt, qrBase64, paymentData);
        return buildCheckoutResponse(booking, orderCode, amount, qrBase64, transferContent, expiresAt, paymentData);
    }

    private void validateBookingForPayment(Booking booking) {
        if (booking.getBookingStatus() == BookingStatus.Cancelled) {
            throw new PaymentException("Booking has been cancelled");
        }
        if (booking.getPaymentStatus() == PaymentStatus.Paid) {
            throw new PaymentException("Booking already paid");
        }
    }

    private PaymentCheckoutResponse buildCheckoutResponse(Booking booking,
                                                          long orderCode,
                                                          BigDecimal amount,
                                                          String qrBase64,
                                                          String transferContent,
                                                          OffsetDateTime expiresAt,
                                                          PayOSPaymentData paymentData) {
        return PaymentCheckoutResponse.builder()
                .success(true)
                .bookingId(booking.getId())
                .orderCode(String.valueOf(orderCode))
                .bookingCode(booking.getBookingCode())
                .amount(amount)
                .qrBase64(qrBase64)
                .checkoutUrl(paymentData.getCheckoutUrl())
                .transferContent(transferContent)
                .expiresAt(expiresAt)
                .bankInfo(PaymentCheckoutResponse.BankInfo.builder()
                        .bank(BANK_NAME)
                        .account(StringUtils.hasText(paymentData.getAccountNumber())
                                ? paymentData.getAccountNumber()
                                : ACCOUNT_NUMBER)
                        .name(StringUtils.hasText(paymentData.getAccountName())
                                ? paymentData.getAccountName()
                                : ACCOUNT_NAME)
                        .build())
                .build();
    }

    @Transactional
    public void completeBooking(PayOSWebhookPayload payload, String rawPayload) {
        if (payload == null) {
            throw new PaymentException("Invalid webhook payload");
        }
        String orderCode = payload.extractOrderCode();
        if (!StringUtils.hasText(orderCode)) {
            throw new PaymentException("Webhook missing orderCode");
        }

        PaymentLog initLog = paymentLogRepository.findTopByProviderTransactionIdOrderByCreatedAtDesc(orderCode)
                .orElseThrow(() -> new PaymentException("Unknown orderCode: " + orderCode));

        Booking booking = bookingRepository.findByIdForUpdate(initLog.getBooking().getId())
                .orElseThrow(() -> new EntityNotFoundException("Booking not found for order " + orderCode));

        validateWebhookPayload(payload, booking, Long.parseLong(orderCode));

        String email = resolveEmail(initLog.getRawMessage());
        bookingService.markBookingPaid(booking, "VietQR");

        initLog.setStatus("PAID");
        initLog.setRawMessage(rawPayload);
        paymentLogRepository.save(initLog);

        if (StringUtils.hasText(email)) {
            ticketEmailService.sendTicket(booking.getId(), email);
        }

        log.info("Payment success recorded for booking={}, orderCode={}",
                booking.getBookingCode(), orderCode);
    }

    public PayOSWebhookPayload verifyPayOSWebhook(String rawPayload,
                                                  String clientId,
                                                  String apiKey,
                                                  String checksum) {
        webhookValidator.validateHeaders(clientId, apiKey, checksum, rawPayload);
        try {
            PayOSWebhookPayload payload = objectMapper.readValue(rawPayload, PayOSWebhookPayload.class);
            String event = payload.getEvent();
            if (!StringUtils.hasText(event)) {
                event = "payment.success";
            }
            if (!"payment.success".equalsIgnoreCase(event) && !"transaction.success".equalsIgnoreCase(event)) {
                PayOSWebhookPayload.WebhookData webhookData = payload.getData();
                boolean successCode = webhookData != null
                        && StringUtils.hasText(webhookData.getCode())
                        && "00".equalsIgnoreCase(webhookData.getCode());
                if (!successCode) {
                    throw new PaymentException("Unsupported webhook event");
                }
            }
            Map<String, Object> dataMap = objectMapper.convertValue(payload.getData(), new TypeReference<Map<String, Object>>() {});
            try {
                webhookValidator.validatePayloadSignature(payload.getSignature(), dataMap);
            } catch (PaymentException ex) {
                log.warn("PayOS signature validation failed: {}. Falling back to API verification.", ex.getMessage());
                long code = Long.parseLong(payload.extractOrderCode());
                PayOSPaymentStatus status = payOSClient.getPaymentStatus(code);
                if (status == null || !"PAID".equalsIgnoreCase(status.getStatus())) {
                    throw new PaymentException("Unable to verify payment status");
                }
                if (status.getAmount() != null && payload.getData() != null
                        && payload.getData().getAmount() != null) {
                    BigDecimal apiAmount = status.getAmount();
                    BigDecimal webhookAmount = BigDecimal.valueOf(payload.getData().getAmount());
                    if (apiAmount.compareTo(webhookAmount) != 0) {
                        throw new PaymentException("PayOS amount mismatch");
                    }
                }
            }
            return payload;
        } catch (IOException ex) {
            throw new PaymentException("Unable to parse webhook payload", ex);
        }
    }

    public void sendTicketEmail(Integer bookingId, String email) {
        ticketEmailService.sendTicket(bookingId, email);
    }

    @Transactional(readOnly = true)
    public byte[] generateTicketPdf(Integer bookingId) {
        return ticketEmailService.generatePdf(bookingId);
    }

    private void validateWebhookPayload(PayOSWebhookPayload payload, Booking booking, long orderCode) {
        PayOSWebhookPayload.WebhookData data = payload.getData();
        if (data == null) {
            throw new PaymentException("Webhook missing data");
        }
        if (!payload.isPaid()) {
            throw new PaymentException("Webhook status is not PAID");
        }

        BigDecimal expectedAmount = booking.getFinalAmount();
        if (expectedAmount != null && data.getAmount() != null) {
            BigDecimal webhookAmount = BigDecimal.valueOf(data.getAmount());
            if (expectedAmount.setScale(0, RoundingMode.HALF_UP).compareTo(webhookAmount) != 0) {
                throw new PaymentException("Webhook amount mismatch");
            }
        }

        String expectedContent = buildTransferContent(booking, orderCode);
        if (StringUtils.hasText(data.getDescription())) {
            String description = data.getDescription().trim();
            String orderCodeStr = String.valueOf(orderCode);
            boolean matchesTransfer = expectedContent.equalsIgnoreCase(description);
            boolean containsOrderCode = description.contains(orderCodeStr);
            if (!matchesTransfer && !containsOrderCode) {
                throw new PaymentException("Webhook transfer content mismatch");
            }
        } else {
            throw new PaymentException("Webhook transfer content mismatch");
        }
    }

    private BigDecimal calculateAmount(Booking booking) {
        List<BookingSeat> seats = bookingSeatRepository.findDetailedByBooking(booking.getId());
        if (seats.isEmpty()) {
            throw new PaymentException("Booking does not contain any seats");
        }
        BigDecimal total = seats.stream()
                .map(BookingSeat::getFinalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Invalid booking amount");
        }
        return total;
    }

    private void persistLog(Booking booking,
                            BigDecimal amount,
                            long orderCode,
                            String transferContent,
                            String email,
                            OffsetDateTime expiresAt,
                            String qrBase64,
                            PayOSPaymentData paymentData) {
        String raw = serializeRawMessage(email, expiresAt, qrBase64, transferContent, orderCode, paymentData);
        PaymentLog logEntry = PaymentLog.builder()
                .booking(booking)
                .provider("PayOS")
                .amount(amount)
                .providerTransactionId(String.valueOf(orderCode))
                .status("PENDING")
                .rawMessage(raw)
                .createdAt(TimeProvider.now())
                .build();
        paymentLogRepository.save(logEntry);
    }

    private String serializeRawMessage(String email,
                                       OffsetDateTime expiresAt,
                                       String qrPayload,
                                       String transferContent,
                                       long orderCode,
                                       PayOSPaymentData paymentData) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", email);
        payload.put("expiresAt", expiresAt.toString());
        payload.put("qr", qrPayload);
        payload.put("transferContent", transferContent);
        payload.put("orderCode", orderCode);
        payload.put("reference", UUID.randomUUID().toString());
        if (paymentData != null) {
            payload.put("checkoutUrl", paymentData.getCheckoutUrl());
            payload.put("paymentLinkId", paymentData.getPaymentLinkId());
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new PaymentException("Unable to serialize payment log payload", ex);
        }
    }

    private String resolveEmail(String rawPayload) {
        try {
            Map<?, ?> map = objectMapper.readValue(rawPayload, Map.class);
            Object email = map.get("email");
            return email != null ? email.toString() : null;
        } catch (Exception ex) {
            log.warn("Unable to parse email from payment log", ex);
            return null;
        }
    }

    private long generateOrderCode() {
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(100, 999);
        return Long.parseLong(String.valueOf(timestamp).substring(4) + random);
    }

    private String buildTransferContent(Booking booking, long orderCode) {
        return "HUB [VE-" + orderCode + "]";
    }

    private String extractBookingName(Booking booking) {
        if (booking == null) {
            return null;
        }
        if (booking.getUser() != null && StringUtils.hasText(booking.getUser().getFullName())) {
            return booking.getUser().getFullName();
        }
        if (booking.getCreatedByStaff() != null && StringUtils.hasText(booking.getCreatedByStaff().getFullName())) {
            return booking.getCreatedByStaff().getFullName();
        }
        return booking.getBookingCode();
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        normalized = normalized.replaceAll("[^A-Za-z0-9 ]", "").trim();
        return normalized.replaceAll("\\s{2,}", " ");
    }

    private String buildReturnUrl(Booking booking) {
        if (!StringUtils.hasText(publicBaseUrl)) {
            return "/movies/confirmation/" + booking.getBookingCode();
        }
        return UriComponentsBuilder.fromHttpUrl(publicBaseUrl)
                .path("/movies/confirmation/{bookingCode}")
                .buildAndExpand(booking.getBookingCode())
                .toUriString();
    }

    private String buildCancelUrl(Booking booking) {
        if (!StringUtils.hasText(publicBaseUrl)) {
            return "/checkout/" + booking.getShowtime().getId();
        }
        return UriComponentsBuilder.fromHttpUrl(publicBaseUrl)
                .path("/checkout/{showtimeId}")
                .buildAndExpand(booking.getShowtime().getId())
                .toUriString();
    }
}
