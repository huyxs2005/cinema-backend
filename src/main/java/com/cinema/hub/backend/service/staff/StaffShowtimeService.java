package com.cinema.hub.backend.service.staff;

import com.cinema.hub.backend.dto.SeatMapItemDto;
import com.cinema.hub.backend.dto.staff.StaffShowtimeFilterDto;
import com.cinema.hub.backend.dto.staff.StaffShowtimeOptionDto;
import com.cinema.hub.backend.dto.staff.StaffShowtimeSummaryDto;
import com.cinema.hub.backend.entity.Showtime;
import com.cinema.hub.backend.repository.ShowtimeRepository;
import com.cinema.hub.backend.repository.staff.ShowtimeOccupancyView;
import com.cinema.hub.backend.repository.staff.StaffShowtimeSeatRepository;
import com.cinema.hub.backend.service.SeatReservationService;
import com.cinema.hub.backend.util.TimeProvider;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final StaffShowtimeSeatRepository staffShowtimeSeatRepository;
    private final SeatReservationService seatReservationService;

    @Transactional(readOnly = true)
    public List<StaffShowtimeSummaryDto> getShowtimes(StaffShowtimeFilterDto filter) {
        List<Showtime> showtimes = showtimeRepository.findAll(buildSpecification(filter),
                Sort.by(Sort.Direction.ASC, "startTime"));
        if (showtimes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, ShowtimeOccupancyView> occupancyMap = loadOccupancy(showtimes);
        return showtimes.stream()
                .map(showtime -> mapToSummary(showtime, occupancyMap.get(showtime.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StaffShowtimeSummaryDto getShowtime(Integer showtimeId) {
        Showtime showtime = showtimeRepository.findByIdWithDetails(showtimeId)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found: " + showtimeId));
        List<SeatMapItemDto> seatMap = seatReservationService.getSeatMap(showtimeId);
        long sellable = seatMap.stream().filter(item -> !"DISABLED".equalsIgnoreCase(item.getStatus())).count();
        long held = seatMap.stream().filter(item -> "HELD".equalsIgnoreCase(item.getStatus())).count();
        long available = seatMap.stream().filter(item -> "AVAILABLE".equalsIgnoreCase(item.getStatus())).count();
        long sold = sellable - held - available;
        ShowtimeOccupancyView fallback = new ShowtimeOccupancyView(showtimeId, seatMap.size(), sellable, sold, held);
        return mapToSummary(showtime, fallback);
    }

    @Transactional(readOnly = true)
    public List<StaffShowtimeOptionDto> getUpcomingOptions(int daysAhead) {
        LocalDateTime now = TimeProvider.now().toLocalDateTime();
        LocalDateTime end = now.plusDays(daysAhead);
        List<Showtime> showtimes = showtimeRepository.findByActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(now, end);
        return showtimes.stream()
                .map(showtime -> StaffShowtimeOptionDto.builder()
                        .showtimeId(showtime.getId())
                        .label(showtime.getMovie().getTitle() + " - "
                                + showtime.getAuditorium().getName())
                        .auditoriumName(showtime.getAuditorium().getName())
                        .startTime(showtime.getStartTime())
                        .endTime(showtime.getEndTime())
                        .build())
                .collect(Collectors.toList());
    }

    private Specification<Showtime> buildSpecification(StaffShowtimeFilterDto filter) {
        LocalDateTime defaultStart = TimeProvider.now().minusHours(6).toLocalDateTime();
        LocalDateTime start = defaultStart;
        LocalDateTime end = defaultStart.plusDays(1);
        if (filter != null) {
            if (filter.getStart() != null) {
                start = normalizeToLocal(filter.getStart());
                end = start.plusDays(1);
            }
            if (filter.getEnd() != null) {
                end = normalizeToLocal(filter.getEnd());
            }
        }
        final LocalDateTime rangeStart = start;
        final LocalDateTime rangeEnd = end;
        Specification<Showtime> spec = Specification.where((root, query, cb) ->
                cb.between(root.get("startTime"), rangeStart, rangeEnd));
        if (filter != null) {
            if (filter.getMovieId() != null) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("movie").get("id"), filter.getMovieId()));
            }
            if (filter.getAuditoriumId() != null) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("auditorium").get("id"), filter.getAuditoriumId()));
            }
            if (filter.getOnlyActive() != null && filter.getOnlyActive()) {
                spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")));
            }
        } else {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("active")));
        }
        return spec;
    }

    private LocalDateTime normalizeToLocal(OffsetDateTime timestamp) {
        return timestamp.atZoneSameInstant(TimeProvider.VN_ZONE_ID).toLocalDateTime();
    }

    private Map<Integer, ShowtimeOccupancyView> loadOccupancy(List<Showtime> showtimes) {
        if (showtimes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Integer> ids = showtimes.stream()
                .map(Showtime::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        OffsetDateTime now = TimeProvider.now();
        List<ShowtimeOccupancyView> rows = staffShowtimeSeatRepository.calculateOccupancy(ids, now);
        return rows.stream()
                .collect(Collectors.toMap(ShowtimeOccupancyView::showtimeId, Function.identity()));
    }

    private StaffShowtimeSummaryDto mapToSummary(Showtime showtime, ShowtimeOccupancyView occupancy) {
        long totalSeats = occupancy != null ? occupancy.totalSeats() : 0;
        long sellableSeats = occupancy != null ? occupancy.sellableSeats() : totalSeats;
        long soldSeats = occupancy != null ? occupancy.soldSeats() : 0;
        long heldSeats = occupancy != null ? occupancy.heldSeats() : 0;
        long availableSeats = Math.max(sellableSeats - soldSeats - heldSeats, 0);
        BigDecimal occupancyPercent = sellableSeats == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(soldSeats)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(sellableSeats), 2, RoundingMode.HALF_UP);
        return StaffShowtimeSummaryDto.builder()
                .showtimeId(showtime.getId())
                .movieId(showtime.getMovie().getId())
                .movieTitle(showtime.getMovie().getTitle())
                .moviePosterUrl(showtime.getMovie().getPosterUrl())
                .auditoriumName(showtime.getAuditorium().getName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .active(Boolean.TRUE.equals(showtime.getActive()))
                .totalSeats(totalSeats)
                .sellableSeats(sellableSeats)
                .soldSeats(soldSeats)
                .heldSeats(heldSeats)
                .availableSeats(availableSeats)
                .occupancyPercent(occupancyPercent)
                .build();
    }
}
