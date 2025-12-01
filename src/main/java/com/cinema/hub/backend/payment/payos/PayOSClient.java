package com.cinema.hub.backend.payment.payos;

import com.cinema.hub.backend.payment.util.PaymentException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PayOSClient {

    private static final String PAYMENT_REQUEST_PATH = "/v2/payment-requests";
    private static final String PAYMENT_STATUS_PATH = "/v2/payment-requests/%s";
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final PayOSConfig payOSConfig;
    private final RestTemplate restTemplate;

    public PayOSClient(PayOSConfig payOSConfig, RestTemplateBuilder restTemplateBuilder) {
        this.payOSConfig = payOSConfig;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public PayOSPaymentData createPaymentRequest(PayOSPaymentRequest request) {
        PayOSCreatePaymentBody payload = buildPayload(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", payOSConfig.getClientId());
        headers.set("x-api-key", payOSConfig.getApiKey());

        HttpEntity<PayOSCreatePaymentBody> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<PayOSCreatePaymentResponse> response = restTemplate.exchange(
                    payOSConfig.getApiBaseUrl() + PAYMENT_REQUEST_PATH,
                    HttpMethod.POST,
                    entity,
                    PayOSCreatePaymentResponse.class
            );
            PayOSCreatePaymentResponse body = response.getBody();
            if (body == null) {
                throw new PaymentException("Empty PayOS response");
            }
            if (!"00".equals(body.getCode())) {
                throw new PaymentException("PayOS error: " + body.getDesc());
            }
            if (body.getData() == null) {
                throw new PaymentException("PayOS response missing data");
            }
            return body.getData();
        } catch (RestClientResponseException ex) {
            log.error("PayOS API error {}", ex.getResponseBodyAsString());
            throw new PaymentException("PayOS API error: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new PaymentException("Unable to reach PayOS", ex);
        }
    }

    public PayOSPaymentStatus getPaymentStatus(long orderCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", payOSConfig.getClientId());
        headers.set("x-api-key", payOSConfig.getApiKey());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<PayOSPaymentStatusResponse> response = restTemplate.exchange(
                    payOSConfig.getApiBaseUrl() + PAYMENT_STATUS_PATH.formatted(orderCode),
                    HttpMethod.GET,
                    entity,
                    PayOSPaymentStatusResponse.class
            );
            PayOSPaymentStatusResponse body = response.getBody();
            if (body == null) {
                throw new PaymentException("Empty PayOS status response");
            }
            if (!"00".equals(body.getCode())) {
                throw new PaymentException("PayOS status error: " + body.getDesc());
            }
            if (body.getData() == null) {
                throw new PaymentException("PayOS status response missing data");
            }
            return body.getData();
        } catch (RestClientResponseException ex) {
            log.error("PayOS status API error {}", ex.getResponseBodyAsString());
            throw new PaymentException("PayOS status API error: " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            throw new PaymentException("Unable to fetch PayOS status", ex);
        }
    }

    private PayOSCreatePaymentBody buildPayload(PayOSPaymentRequest request) {
        PayOSCreatePaymentBody body = new PayOSCreatePaymentBody();
        body.setOrderCode(request.getOrderCode());
        body.setAmount(request.getAmount());
        body.setDescription(request.getDescription());
        body.setReturnUrl(request.getReturnUrl());
        body.setCancelUrl(request.getCancelUrl());
        body.setSignature(buildSignature(body));
        return body;
    }

    private String buildSignature(PayOSCreatePaymentBody body) {
        Map<String, String> values = new TreeMap<>();
        values.put("amount", String.valueOf(body.getAmount()));
        values.put("cancelUrl", body.getCancelUrl());
        values.put("description", body.getDescription());
        values.put("orderCode", String.valueOf(body.getOrderCode()));
        values.put("returnUrl", body.getReturnUrl());
        String data = values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        return hmacSha256(data, payOSConfig.getChecksumKey());
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
            throw new PaymentException("Unable to generate PayOS signature", ex);
        }
    }

    @Data
    private static class PayOSCreatePaymentBody {
        private long orderCode;
        private long amount;
        private String description;
        private String returnUrl;
        private String cancelUrl;
        private String signature;
    }
}
