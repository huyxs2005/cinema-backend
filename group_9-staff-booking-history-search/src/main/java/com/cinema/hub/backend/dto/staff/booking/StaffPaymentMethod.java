package com.cinema.hub.backend.dto.staff.booking;

public enum StaffPaymentMethod {
    CASH,
    VIETQR;

    public static StaffPaymentMethod from(String value) {
        if (value == null) {
            return CASH;
        }
        try {
            return StaffPaymentMethod.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CASH;
        }
    }
}
