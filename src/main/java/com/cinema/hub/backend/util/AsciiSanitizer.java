package com.cinema.hub.backend.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class AsciiSanitizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private AsciiSanitizer() {
    }

    public static String toAscii(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String stripped = DIACRITICS.matcher(normalized).replaceAll("");
        stripped = stripped
                .replace('Đ', 'D')
                .replace('đ', 'd');
        return stripped;
    }
}
