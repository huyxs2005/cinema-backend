package com.cinema.hub.backend.payment.payos;

import com.cinema.hub.backend.payment.util.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PayOSWebhookValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final PayOSConfig payOSConfig;

    public void validateHeaders(String clientId,
                                String apiKey,
                                String checksumHeader,
                                String payload) {
        if (StringUtils.hasText(clientId) && !payOSConfig.getClientId().equals(clientId)) {
            throw new PaymentException("Invalid PayOS client id");
        }
        if (StringUtils.hasText(apiKey) && !payOSConfig.getApiKey().equals(apiKey)) {
            throw new PaymentException("Invalid PayOS api key");
        }
        if (StringUtils.hasText(checksumHeader)) {
            String expected = hmacSha256(payload, payOSConfig.getChecksumKey());
            if (!expected.equalsIgnoreCase(checksumHeader)) {
                throw new PaymentException("Invalid webhook checksum");
            }
        }
    }

    public void validatePayloadSignature(String signature, Map<String, Object> data) {
        if (!StringUtils.hasText(signature)) {
            throw new PaymentException("Webhook missing signature");
        }
        if (data == null || data.isEmpty()) {
            throw new PaymentException("Webhook missing data");
        }
        String canonical = data.entrySet().stream()
                .filter(entry -> entry.getValue() != null && StringUtils.hasText(entry.getValue().toString()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + normalize(entry.getValue()))
                .collect(Collectors.joining("&"));
        String expected = hmacSha256(canonical, payOSConfig.getChecksumKey());
        if (!expected.equalsIgnoreCase(signature)) {
            throw new PaymentException("Webhook signature mismatch");
        }
    }

    private String normalize(Object value) {
        if (value instanceof Number number) {
            return new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }

    private String hmacSha256(String payload, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new PaymentException("Unable to verify webhook signature", ex);
        }
    }
}
