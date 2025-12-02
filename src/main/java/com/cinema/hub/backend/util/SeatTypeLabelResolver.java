package com.cinema.hub.backend.util;

import java.util.Locale;

public final class SeatTypeLabelResolver {

    private SeatTypeLabelResolver() {
    }

    public static String localize(String seatType) {
        if (seatType == null || seatType.isBlank()) {
            return "";
        }
        String normalized = seatType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "standard" -> "thường";
            case "vip" -> "vip";
            case "couple" -> "đôi";
            default -> seatType;
        };
    }
}
