package com.cinema.hub.backend.util;

import java.util.Locale;
import org.springframework.util.StringUtils;

public final class PaymentMethodNormalizer {

    private PaymentMethodNormalizer() {
    }

    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        String compact = key.replaceAll("[\\s_-]+", "");
        switch (compact) {
            case "cash":
                return "Cash";
            case "transfer":
            case "banktransfer":
            case "vietqr":
                return "Transfer";
            case "momo":
                return "MoMo";
            case "card":
            case "creditcard":
            case "debitcard":
                return "Card";
            case "vnpay":
                return "VNPay";
            default:
                throw new IllegalArgumentException("Unsupported payment method: " + raw);
        }
    }
}
