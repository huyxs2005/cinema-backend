package com.cinema.hub.backend.service;

import com.cinema.hub.backend.repository.SeatHoldRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatHoldCleanupService {

    private static final String HELD_STATUS = "Held";

    private final SeatHoldRepository seatHoldRepository;

    @Scheduled(fixedDelayString = "${cinema.seat-hold.cleanup-interval-ms:60000}")
    @Transactional
    public void releaseExpiredHolds() {
        OffsetDateTime now = OffsetDateTime.now();
        int removed = seatHoldRepository.deleteExpiredHolds(HELD_STATUS, now);
        if (removed > 0) {
            log.info("Released {} expired seat holds (cutoff {}).", removed, now);
        }
    }
}
