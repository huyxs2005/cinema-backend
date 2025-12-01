package com.cinema.hub.backend.service.job;

import com.cinema.hub.backend.service.SeatReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatHoldCleanupJob {

    private final SeatReservationService seatReservationService;

    @Scheduled(cron = "0 * * * * ?")
    public void releaseExpiredHolds() {
        int released = seatReservationService.expireSeatHolds();
        if (released > 0) {
            log.info("Released {} expired seat holds", released);
        }
    }
}
