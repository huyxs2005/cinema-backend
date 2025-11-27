package com.cinema.hub.backend.util;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class BookingCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private BookingCodeGenerator() {
    }

    public static String newCode(OffsetDateTime timestamp) {
        String base = FORMATTER.format(timestamp);
        int suffix = RANDOM.nextInt(900) + 100;
        return "STF" + base + suffix;
    }
}
