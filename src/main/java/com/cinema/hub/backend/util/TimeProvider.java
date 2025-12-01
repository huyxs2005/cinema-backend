package com.cinema.hub.backend.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Centralized helper for Vietnam time handling.
 */
public final class TimeProvider {

    public static final ZoneId VN_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final ZoneOffset VN_ZONE_OFFSET = ZoneOffset.ofHours(7);

    private TimeProvider() {
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(VN_ZONE_ID);
    }
}
