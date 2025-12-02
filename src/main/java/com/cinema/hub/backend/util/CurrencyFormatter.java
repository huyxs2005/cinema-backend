package com.cinema.hub.backend.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyFormatter {

    private static final Locale VI_LOCALE = new Locale("vi", "VN");

    private CurrencyFormatter() {
    }

    public static String format(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        NumberFormat formatter = NumberFormat.getInstance(VI_LOCALE);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(0);
        return formatter.format(safeAmount) + " đ";
    }
}
