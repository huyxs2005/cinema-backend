package com.cinema.hub.backend.service.impl;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.showtime.ShowtimeDayGroupResponse;
import com.cinema.hub.backend.dto.showtime.ShowtimeGroupedResponse;
import com.cinema.hub.backend.dto.showtime.ShowtimeOccurrenceResponse;
import com.cinema.hub.backend.dto.showtime.ShowtimeRequest;
import com.cinema.hub.backend.dto.showtime.ShowtimeResponse;
import com.cinema.hub.backend.entity.Auditorium;
import com.cinema.hub.backend.entity.Movie;
import com.cinema.hub.backend.entity.Seat;
import com.cinema.hub.backend.entity.Showtime;
import com.cinema.hub.backend.entity.ShowtimeSeat;
import com.cinema.hub.backend.mapper.ShowtimeMapper;
import com.cinema.hub.backend.repository.AuditoriumRepository;
import com.cinema.hub.backend.repository.MovieRepository;
import com.cinema.hub.backend.repository.SeatRepository;
import com.cinema.hub.backend.repository.SeatHoldRepository;
import com.cinema.hub.backend.repository.ShowtimeRepository;
import com.cinema.hub.backend.repository.ShowtimeSeatRepository;
import com.cinema.hub.backend.service.ShowtimeService;
import com.cinema.hub.backend.specification.ShowtimeSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.cinema.hub.backend.util.TimeProvider;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import com.cinema.hub.backend.web.view.SeatSelectionShowtimeView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final String DEFAULT_SEAT_STATUS = "Available";
    private static final int DEFAULT_CLEANUP_MINUTES = 15;
    private static final BigDecimal WEEKDAY_MORNING_PRICE = new BigDecimal("2000");
    private static final BigDecimal WEEKDAY_AFTERNOON_PRICE = new BigDecimal("2000");
    private static final BigDecimal WEEKDAY_EVENING_PRICE = new BigDecimal("2000");
    private static final BigDecimal WEEKDAY_LATE_PRICE = new BigDecimal("2000");
    private static final BigDecimal WEEKEND_MORNING_PRICE = new BigDecimal("2000");
    private static final BigDecimal WEEKEND_AFTERNOON_PRICE = new BigDecimal("2000");
    private static final BigDecimal WEEKEND_EVENING_PRICE = new BigDecimal("2000");
    private static final BigDecimal WEEKEND_LATE_PRICE = new BigDecimal("2000");
    private static final DateTimeFormatter TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final ShowtimeMapper showtimeMapper;

    @Override
    public List<ShowtimeResponse> create(ShowtimeRequest request) {
        Movie movie = requireMovie(request.getMovieId());
        Auditorium auditorium = requireAuditorium(request.getAuditoriumId());
        List<ScheduleSlot> slots = buildScheduleSlots(movie, request);
        slots.forEach(slot -> enforceNoConflict(auditorium.getId(), slot.start(), slot.end(), null));

        List<ShowtimeResponse> responses = new ArrayList<>();
        for (ScheduleSlot slot : slots) {
            Showtime showtime = new Showtime();
            showtime.setMovie(movie);
            showtime.setAuditorium(auditorium);
            showtime.setStartTime(slot.start());
            showtime.setEndTime(slot.end());
            showtime.setBasePrice(determineBasePrice(slot.start()));
            showtime.setActive(Boolean.TRUE.equals(request.getActive()));
            Showtime saved = showtimeRepository.save(showtime);
            generateShowtimeSeats(saved);
            responses.add(showtimeMapper.toResponse(saved));
        }
        return responses;
    }

    @Override
    public ShowtimeResponse update(int id, ShowtimeRequest request) {
        Showtime showtime = getEntity(id);
        Movie movie = requireMovie(request.getMovieId());
        Auditorium auditorium = requireAuditorium(request.getAuditoriumId());
        validateStartTime(request.getStartTime());
        LocalDateTime endTime = calculateEndTime(movie, request.getStartTime(), request.getCleanupMinutes());
        enforceNoConflict(auditorium.getId(), request.getStartTime(), endTime, id);

        showtime.setMovie(movie);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setBasePrice(determineBasePrice(request.getStartTime()));
        showtime.setActive(request.getActive());
        return showtimeMapper.toResponse(showtimeRepository.save(showtime));
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse get(int id) {
        return showtimeMapper.toResponse(getEntity(id));
    }

    @Override
    public void delete(int id) {
        Showtime showtime = getEntity(id);
        seatHoldRepository.deleteByShowtimeId(showtime.getId());
        showtimeSeatRepository.deleteByShowtime_Id(showtime.getId());
        showtimeRepository.delete(showtime);
    }

    @Override
    public ShowtimeResponse updateActiveStatus(int id, boolean active) {
        Showtime showtime = getEntity(id);
        showtime.setActive(active);
        return showtimeMapper.toResponse(showtimeRepository.save(showtime));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> search(Integer movieId,
                                                 Integer auditoriumId,
                                                 Boolean active,
                                                 LocalDate fromDate,
                                                 LocalDate toDate,
                                                 String keyword,
                                                 Pageable pageable) {
        Page<Showtime> page = showtimeRepository.findAll(
                ShowtimeSpecifications.filter(movieId, auditoriumId, active, fromDate, toDate, keyword), pageable);
        return PageResponse.from(page.map(showtimeMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getUpcomingShowtimesForMovie(int movieId, LocalDate fromDate, LocalDate toDate) {
        LocalDate start = fromDate != null ? fromDate : LocalDate.now(TimeProvider.VN_ZONE_ID);
        LocalDate end = toDate != null ? toDate : start.plusDays(6);
        if (end.isBefore(start)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }
        LocalDateTime fromDateTime = start.atStartOfDay();
        LocalDateTime toDateTime = end.plusDays(1).atStartOfDay().minusSeconds(1);
        return showtimeRepository
                .findByMovie_IdAndActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(movieId, fromDateTime, toDateTime)
                .stream()
                .map(showtimeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getShowtimesByDate(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now(TimeProvider.VN_ZONE_ID);
        LocalDateTime from = target.atStartOfDay();
        LocalDateTime to = target.plusDays(1).atStartOfDay().minusSeconds(1);
        return showtimeRepository
                .findByActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(from, to)
                .stream()
                .map(showtimeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeGroupedResponse> groupByMovie(Integer movieId,
                                                      Integer auditoriumId,
                                                      Boolean active,
                                                      LocalDate fromDate,
                                                      LocalDate toDate,
                                                      String keyword) {
        List<Showtime> showtimes = showtimeRepository.findAll(
                ShowtimeSpecifications.filter(movieId, auditoriumId, active, fromDate, toDate, keyword),
                Sort.by(Sort.Direction.ASC, "startTime"));
        if (showtimes.isEmpty()) {
            return List.of();
        }
        Map<LocalTime, Map<DayOfWeek, List<Showtime>>> grouped = new TreeMap<>();
        for (Showtime showtime : showtimes) {
            LocalDateTime start = showtime.getStartTime();
            if (start == null) {
                continue;
            }
            grouped.computeIfAbsent(start.toLocalTime(), time -> new EnumMap<>(DayOfWeek.class))
                    .computeIfAbsent(start.getDayOfWeek(), day -> new ArrayList<>())
                    .add(showtime);
        }
        List<ShowtimeGroupedResponse> responses = new ArrayList<>();
        for (Map.Entry<LocalTime, Map<DayOfWeek, List<Showtime>>> entry : grouped.entrySet()) {
            List<ShowtimeDayGroupResponse> dayGroups = buildDayGroups(entry.getValue());
            int total = dayGroups.stream()
                    .mapToInt(ShowtimeDayGroupResponse::totalShowtimes)
                    .sum();
            responses.add(new ShowtimeGroupedResponse(
                    entry.getKey(),
                    TIME_LABEL_FORMATTER.format(entry.getKey()),
                    total,
                    dayGroups));
        }
        return responses;
    }

    private List<ShowtimeDayGroupResponse> buildDayGroups(Map<DayOfWeek, List<Showtime>> dayMap) {
        List<ShowtimeDayGroupResponse> responses = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            List<Showtime> dayShowtimes = dayMap.get(day);
            if (dayShowtimes == null || dayShowtimes.isEmpty()) {
                continue;
            }
            dayShowtimes.sort(Comparator.comparing(Showtime::getStartTime,
                    Comparator.nullsLast(LocalDateTime::compareTo)));
            List<ShowtimeOccurrenceResponse> occurrences = dayShowtimes.stream()
                    .map(this::toOccurrenceResponse)
                    .toList();
            responses.add(new ShowtimeDayGroupResponse(
                    day.getValue(),
                    formatDayLabel(day),
                    occurrences.size(),
                    occurrences));
        }
        return responses;
    }

    private ShowtimeOccurrenceResponse toOccurrenceResponse(Showtime showtime) {
        LocalDateTime start = showtime.getStartTime();
        LocalDate showDate = start != null ? start.toLocalDate() : null;
        Auditorium auditorium = showtime.getAuditorium();
        Movie movie = showtime.getMovie();
        return new ShowtimeOccurrenceResponse(
                showtime.getId(),
                showDate,
                showDate != null ? DATE_LABEL_FORMATTER.format(showDate) : "",
                auditorium != null ? auditorium.getId() : null,
                auditorium != null ? auditorium.getName() : null,
                showtime.getActive(),
                start,
                showtime.getEndTime(),
                movie != null ? movie.getId() : null,
                movie != null ? movie.getTitle() : null,
                movie != null ? movie.getOriginalTitle() : null);
    }

    private String formatDayLabel(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ nhật";
        };
    }

    private Movie requireMovie(Integer id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found: " + id));
    }

    private Auditorium requireAuditorium(Integer id) {
        return auditoriumRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auditorium not found: " + id));
    }

    private Showtime getEntity(int id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Showtime not found: " + id));
    }

    private LocalDateTime calculateEndTime(Movie movie, LocalDateTime startTime, Integer cleanupMinutes) {
        if (startTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time is required");
        }
        int duration = movie.getDurationMinutes() != null ? movie.getDurationMinutes() : 0;
        int cleanup = cleanupMinutes != null ? cleanupMinutes : DEFAULT_CLEANUP_MINUTES;
        LocalDateTime endTime = startTime.plusMinutes(duration + Math.max(cleanup, 0));
        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
        return endTime;
    }

    private void validateStartTime(LocalDateTime startTime) {
        if (startTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start time is required");
        }
        LocalDateTime now = LocalDateTime.now(TimeProvider.VN_ZONE_ID);
        if (startTime.isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giờ bắt đầu phải sau thời điểm hiện tại");
        }
        LocalTime localTime = startTime.toLocalTime();
        LocalTime earliest = LocalTime.of(8, 0);
        LocalTime latest = LocalTime.of(22, 0);
        if (localTime.isBefore(earliest) || localTime.isAfter(latest)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Giờ bắt đầu phải nằm trong khoảng từ 08:00 đến 22:00.");
        }
    }

    private void enforceNoConflict(Integer auditoriumId,
                                   LocalDateTime start,
                                   LocalDateTime end,
                                   Integer excludeId) {
        boolean conflict = showtimeRepository.existsConflictingShowtime(auditoriumId, start, end, excludeId);
        if (conflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PhÃ²ng chiáº¿u Ä‘Ã£ cÃ³ suáº¥t chiáº¿u khÃ¡c trong khoáº£ng thá»i gian nÃ y.");
        }
    }

    private void generateShowtimeSeats(Showtime showtime) {
        List<Seat> seats = seatRepository
                .findByAuditorium_IdAndActiveTrueOrderByRowLabelAscSeatNumberAsc(showtime.getAuditorium().getId());
        if (seats.isEmpty()) {
            return;
        }
        BigDecimal basePrice = showtime.getBasePrice();
        List<ShowtimeSeat> seatSnapshots = seats.stream()
                .map(seat -> ShowtimeSeat.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .effectivePrice(calculateSeatPrice(basePrice,
                                seat.getSeatType() != null ? seat.getSeatType().getPriceMultiplier() : null))
                        .status(DEFAULT_SEAT_STATUS)
                        .build())
                .toList();
        showtimeSeatRepository.saveAll(seatSnapshots);
    }

    private BigDecimal calculateSeatPrice(BigDecimal basePrice, BigDecimal multiplier) {
        BigDecimal effectiveMultiplier = multiplier != null ? multiplier : BigDecimal.ONE;
        return basePrice.multiply(effectiveMultiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private List<ScheduleSlot> buildScheduleSlots(Movie movie, ShowtimeRequest request) {
        LocalDateTime baseStart = request.getStartTime();
        validateStartTime(baseStart);
        RepeatMode mode = RepeatMode.from(request.getRepeatMode());
        LocalDate repeatUntil = request.getRepeatUntil() != null ? request.getRepeatUntil() : baseStart.toLocalDate();
        if (repeatUntil.isBefore(baseStart.toLocalDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NgÃ y káº¿t thÃºc pháº£i sau hoáº·c báº±ng ngÃ y báº¯t Ä‘áº§u.");
        }
        List<DayOfWeek> customDays = resolveCustomDays(request.getRepeatDays(), baseStart.getDayOfWeek());
        List<LocalDateTime> startTimes = expandStartTimes(baseStart, repeatUntil, mode, customDays);
        if (startTimes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KhÃ´ng tÃ¬m tháº¥y ngÃ y phÃ¹ há»£p cho tÃ¹y chá»n Ä‘Ã£ chá»n.");
        }
        List<ScheduleSlot> slots = new ArrayList<>();
        for (LocalDateTime slotStart : startTimes) {
            LocalDateTime slotEnd = calculateEndTime(movie, slotStart, request.getCleanupMinutes());
            slots.add(new ScheduleSlot(slotStart, slotEnd));
        }
        return slots;
    }

    private List<LocalDateTime> expandStartTimes(LocalDateTime start,
                                                 LocalDate repeatUntil,
                                                 RepeatMode mode,
                                                 List<DayOfWeek> customDays) {
        if (mode == RepeatMode.NONE) {
            return List.of(start);
        }
        List<LocalDateTime> result = new ArrayList<>();
        LocalDate currentDate = start.toLocalDate();
        LocalTime startTime = start.toLocalTime();
        while (!currentDate.isAfter(repeatUntil)) {
            DayOfWeek currentDay = currentDate.getDayOfWeek();
            boolean include = switch (mode) {
                case WHOLE_WEEK -> true;
                case WEEKDAY -> isWeekday(currentDay);
                case WEEKEND -> isWeekend(currentDay);
                case CUSTOM -> customDays.contains(currentDay);
                default -> false;
            };
            if (include) {
                result.add(LocalDateTime.of(currentDate, startTime));
            }
            currentDate = currentDate.plusDays(1);
        }
        return result;
    }

    private BigDecimal determineBasePrice(LocalDateTime startTime) {
        LocalDateTime effectiveTime = startTime != null ? startTime : LocalDateTime.now(TimeProvider.VN_ZONE_ID);
        boolean weekend = isWeekend(effectiveTime.getDayOfWeek());
        int hour = effectiveTime.getHour();
        if (hour < 12) {
            return weekend ? WEEKEND_MORNING_PRICE : WEEKDAY_MORNING_PRICE;
        }
        if (hour < 17) {
            return weekend ? WEEKEND_AFTERNOON_PRICE : WEEKDAY_AFTERNOON_PRICE;
        }
        if (hour < 23) {
            return weekend ? WEEKEND_EVENING_PRICE : WEEKDAY_EVENING_PRICE;
        }
        return weekend ? WEEKEND_LATE_PRICE : WEEKDAY_LATE_PRICE;
    }

    @Override
    @Transactional(readOnly = true)
    public SeatSelectionShowtimeView getSeatSelectionDetails(int showtimeId) {
        Showtime showtime = showtimeRepository.findByIdWithDetails(showtimeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Showtime not found"));
        return SeatSelectionShowtimeView.fromEntity(showtime);
    }

    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY
                || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isWeekday(DayOfWeek dayOfWeek) {
        return !isWeekend(dayOfWeek);
    }

    private enum RepeatMode {
        NONE,
        WHOLE_WEEK,
        WEEKDAY,
        WEEKEND,
        CUSTOM;

        static RepeatMode from(String value) {
            if (value == null || value.isBlank()) {
                return NONE;
            }
            try {
                return RepeatMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return NONE;
            }
        }
    }

    private List<DayOfWeek> resolveCustomDays(List<Integer> repeatDays, DayOfWeek fallback) {
        if (repeatDays == null || repeatDays.isEmpty()) {
            return List.of(fallback);
        }
        List<DayOfWeek> resolved = new ArrayList<>();
        for (Integer value : repeatDays) {
            if (value == null) {
                continue;
            }
            if (value >= 1 && value <= 7) {
                DayOfWeek dayOfWeek = DayOfWeek.of(value == 0 ? 7 : value);
                if (!resolved.contains(dayOfWeek)) {
                    resolved.add(dayOfWeek);
                }
            }
        }
        return resolved.isEmpty() ? List.of(fallback) : resolved;
    }

    private record ScheduleSlot(LocalDateTime start, LocalDateTime end) {
    }
}

