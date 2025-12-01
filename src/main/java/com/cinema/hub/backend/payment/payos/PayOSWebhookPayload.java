package com.cinema.hub.backend.payment.payos;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayOSWebhookPayload {

    private String code;
    private String desc;
    @JsonAlias({"event", "eventType"})
    private String event;
    private WebhookData data;
    private String signature;

    public String extractOrderCode() {
        if (data == null) {
            return null;
        }
        if (data.getOrderId() != null) {
            return data.getOrderId();
        }
        if (data.getOrderCode() != null) {
            return String.valueOf(data.getOrderCode());
        }
        return null;
    }

    public boolean isPaid() {
        if (data == null) {
            return false;
        }
        if ("PAID".equalsIgnoreCase(data.getStatus())
                || "PAID".equalsIgnoreCase(data.getTransactionStatus())) {
            return true;
        }
        if ("00".equalsIgnoreCase(data.getCode())) {
            return true;
        }
        if ("success".equalsIgnoreCase(data.getDesc())) {
            return true;
        }
        return false;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookData {
        private Long orderCode;
        private String orderId;
        private Long amount;
        private String description;
        private String status;
        private String transactionId;
        private String transactionStatus;
        private String paidAt;
        private String accountNumber;
        private String reference;
        private String transactionDateTime;
        private String currency;
        private String paymentLinkId;
        private String code;
        private String desc;
    }
}
