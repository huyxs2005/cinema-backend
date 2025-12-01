package com.cinema.hub.backend.controller;

public final class ApiEndpoints {

    private ApiEndpoints() {
    }

    public static final class SeatReservation {
        public static final String SEAT_LAYOUT = "/api/showtimes/{showtimeId}/seats";
        public static final String HOLD_SEATS = "/api/showtimes/{showtimeId}/holds";
        public static final String CREATE_BOOKING = "/api/bookings";
        public static final String CANCEL_BOOKING = "/api/bookings/{bookingId}/cancel";
        public static final String RELEASE_HOLD = "/api/showtimes/{showtimeId}/holds/{holdToken}";
        public static final String RELEASE_HOLD_BEACON = "/api/showtimes/{showtimeId}/holds/{holdToken}/release";
        public static final String RELEASE_USER_HOLDS = "/api/showtimes/holds/release";

        private SeatReservation() {
        }
    }

}
