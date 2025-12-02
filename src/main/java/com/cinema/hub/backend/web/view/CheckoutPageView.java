package com.cinema.hub.backend.web.view;

import com.cinema.hub.backend.util.CurrencyFormatter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CheckoutPageView {

    private final String holdToken;
    private final OffsetDateTime expiresAt;
    private final Integer bookingId;
    private final String bookingCode;
    private final SeatSelectionShowtimeView showtime;
    private final List<SeatSelectionItemView> seats;
    private final List<Integer> seatIds;
    private final BigDecimal total;
    private final int seatCount;

    public String getFormattedTotal() {
        return CurrencyFormatter.format(total);
    }
}
