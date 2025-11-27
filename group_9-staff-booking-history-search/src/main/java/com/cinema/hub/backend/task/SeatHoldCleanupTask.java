package com.cinema.hub.backend.task;

import com.cinema.hub.backend.entity.SeatHold;
import com.cinema.hub.backend.repository.SeatHoldRepository;
import com.cinema.hub.backend.util.SeatHoldStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeatHoldCleanupTask {

    private final SeatHoldRepository seatHoldRepository;

    @Scheduled(fixedDelayString = "${staff.seats.hold-cleanup-ms:60000}")
    public void expireStaleHolds() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<SeatHold> stale = seatHoldRepository.findByStatusAndExpiresAtBefore(SeatHoldStatus.HELD, now);
        if (stale.isEmpty()) {
            return;
        }
        stale.forEach(hold -> hold.setStatus(SeatHoldStatus.EXPIRED));
        seatHoldRepository.saveAll(stale);
        log.debug("Expired {} stale seat holds", stale.size());
    }
}
