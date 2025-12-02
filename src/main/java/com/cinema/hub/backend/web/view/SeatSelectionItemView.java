package com.cinema.hub.backend.web.view;

import com.cinema.hub.backend.util.CurrencyFormatter;
import com.cinema.hub.backend.util.SeatTypeLabelResolver;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeatSelectionItemView {
    private final String label;
    private final BigDecimal price;
    private final String seatType;

    public String getSeatTypeLabel() {
        return SeatTypeLabelResolver.localize(seatType);
    }

    public String getSeatLabelWithType() {
        if (seatType == null || seatType.isBlank()) {
            return label;
        }
        return label + " (" + getSeatTypeLabel() + ")";
    }

    public String getFormattedPrice() {
        return CurrencyFormatter.format(price);
    }
}
