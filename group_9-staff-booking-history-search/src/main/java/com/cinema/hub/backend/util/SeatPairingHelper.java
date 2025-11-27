package com.cinema.hub.backend.util;

import com.cinema.hub.backend.entity.Seat;
import org.springframework.util.StringUtils;

public final class SeatPairingHelper {

    private static final String COUPLE_KEYWORD = "COUPLE";

    private SeatPairingHelper() {
    }

    public static boolean isCoupleSeat(Seat seat) {
        if (seat == null || seat.getSeatType() == null) {
            return false;
        }
        String typeName = seat.getSeatType().getName();
        return StringUtils.hasText(typeName) && typeName.trim().toUpperCase().contains(COUPLE_KEYWORD);
    }

    public static String buildPairId(Seat seat) {
        if (!isCoupleSeat(seat)) {
            return null;
        }
        String row = StringUtils.hasText(seat.getRowLabel()) ? seat.getRowLabel().trim().toUpperCase() : "ROW";
        Integer seatNumber = seat.getSeatNumber() != null ? seat.getSeatNumber() : 0;
        int pairIndex = (seatNumber + 1) / 2;
        return row + "-" + pairIndex;
    }
}
