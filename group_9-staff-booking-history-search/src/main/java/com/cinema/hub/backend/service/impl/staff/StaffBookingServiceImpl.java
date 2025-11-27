package com.cinema.hub.backend.service.impl.staff;

import com.cinema.hub.backend.dto.staff.booking.StaffBookingComboDto;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingDetailView;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingRequest;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingResult;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingSeatDto;
import com.cinema.hub.backend.dto.staff.booking.StaffBookingStatusResponse;
import com.cinema.hub.backend.dto.staff.booking.StaffComboOptionDto;
import com.cinema.hub.backend.dto.staff.booking.StaffComboSelection;
import com.cinema.hub.backend.dto.staff.booking.StaffPaymentMethod;
import com.cinema.hub.backend.dto.staff.booking.StaffVietQrInfo;
import com.cinema.hub.backend.dto.staff.seat.StaffSeatDto;
import com.cinema.hub.backend.dto.staff.seat.StaffSeatMapView;
import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.BookingCombo;
import com.cinema.hub.backend.entity.BookingSeat;
import com.cinema.hub.backend.entity.Combo;
import com.cinema.hub.backend.entity.PaymentLog;
import com.cinema.hub.backend.entity.SeatHold;
import com.cinema.hub.backend.entity.Showtime;
import com.cinema.hub.backend.entity.ShowtimeSeat;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.BookingSeatRepository;
import com.cinema.hub.backend.repository.ComboRepository;
import com.cinema.hub.backend.repository.PaymentLogRepository;
import com.cinema.hub.backend.repository.SeatHoldRepository;
import com.cinema.hub.backend.repository.ShowtimeRepository;
import com.cinema.hub.backend.repository.ShowtimeSeatRepository;
import com.cinema.hub.backend.service.staff.StaffBookingService;
import com.cinema.hub.backend.service.support.SeatLayoutService;
import com.cinema.hub.backend.util.BookingCodeGenerator;
import com.cinema.hub.backend.util.SeatHoldStatus;
import com.cinema.hub.backend.util.SeatPairingHelper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffBookingServiceImpl implements StaffBookingService {

    private static final String STATUS_BOOKED = "Booked";
    private static final String STATUS_AVAILABLE = "Available";
    private static final String STATUS_DISABLED = "Disabled";
    private static final int SEAT_HOLD_MINUTES = 10;

    private static final String BOOKING_STATUS_PENDING = "Pending";
    private static final String BOOKING_STATUS_CONFIRMED = "Confirmed";
    private static final String BOOKING_STATUS_CANCELLED = "Cancelled";
    private static final String PAYMENT_STATUS_PAID = "Paid";
    private static final String PAYMENT_STATUS_UNPAID = "Unpaid";

    private static final String PROVIDER_VIETQR = "VietQR";
    private static final String PROVIDER_CASH = "CashDesk";
    private static final int QR_TIMEOUT_MINUTES = 10;
    private static final String VIETQR_BANK_CODE = "970422";
    private static final String VIETQR_ACCOUNT = "0931630902";
    private static final String VIETQR_ACCOUNT_NAME = "DAO NAM HAI";

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ComboRepository comboRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatLayoutService seatLayoutService;

    @Override
    public StaffSeatMapView loadSeatMapForShowtime(Integer showtimeId, UserAccount staffUser) {
        Showtime showtime = requireShowtime(showtimeId);
        List<ShowtimeSeat> showtimeSeats = showtimeSeatRepository
                .findByShowtime_IdOrderBySeat_RowLabelAscSeat_SeatNumberAsc(showtimeId);
        if (showtimeSeats.isEmpty()) {
            showtimeSeats = seatLayoutService.ensureShowtimeSeats(showtime);
        }
        Set<Integer> bookedSeatIds = bookingSeatRepository.findLockedSeatIdsForShowtime(showtimeId);
        OffsetDateTime now = currentTime();
        Set<Integer> heldSeatIds = seatHoldRepository.findActiveHeldSeatIds(showtimeId, now);
        Set<Integer> heldByCurrentUser = staffUser != null
                ? seatHoldRepository.findActiveHeldSeatIdsForUser(showtimeId, staffUser.getId(), now)
                : Set.of();
        int heldCount = (int) heldSeatIds.stream()
                .filter(id -> !bookedSeatIds.contains(id))
                .count();

        List<StaffSeatDto> seats = showtimeSeats.stream()
                .map(seat -> {
                    boolean isCouple = SeatPairingHelper.isCoupleSeat(seat.getSeat());
                    String pairId = isCouple ? SeatPairingHelper.buildPairId(seat.getSeat()) : null;
                    return StaffSeatDto.builder()
                            .showtimeSeatId(seat.getId())
                            .rowLabel(seat.getSeat().getRowLabel())
                            .seatNumber(seat.getSeat().getSeatNumber())
                            .seatType(seat.getSeat().getSeatType() != null ? seat.getSeat().getSeatType().getName() : "Standard")
                            .price(seat.getEffectivePrice())
                            .status(resolveSeatStatus(seat, bookedSeatIds, heldSeatIds))
                            .couple(isCouple)
                            .couplePairId(pairId)
                            .heldByCurrentUser(heldByCurrentUser.contains(seat.getId()))
                            .build();
                })
                .toList();

        return StaffSeatMapView.builder()
                .showtimeId(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .auditoriumName(showtime.getAuditorium().getName())
                .startTime(showtime.getStartTime())
                .totalSeats(showtimeSeats.size())
                .soldSeats(bookedSeatIds.size())
                .heldSeats(heldCount)
                .seats(seats)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffComboOptionDto> getActiveCombos() {
        return comboRepository.findByActiveTrueOrderByPriceAsc()
                .stream()
                .map(combo -> StaffComboOptionDto.builder()
                        .comboId(combo.getId())
                        .name(combo.getName())
                        .description(combo.getDescription())
                        .price(combo.getPrice())
                        .build())
                .toList();
    }

    @Override
    public List<Integer> holdSeats(Integer showtimeId, List<Integer> seatIds, UserAccount staffUser) {
        if (staffUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ");
        }
        List<Integer> requestedSeats = sanitizeSeatSelections(seatIds);
        if (requestedSeats.isEmpty()) {
            return List.of();
        }
        Showtime showtime = requireShowtime(showtimeId);
        List<ShowtimeSeat> seats = fetchShowtimeSeats(showtime.getId(), requestedSeats);
        ensureSeatsAvailable(seats, staffUser);
        OffsetDateTime now = currentTime();
        OffsetDateTime expiresAt = now.plusMinutes(SEAT_HOLD_MINUTES);
        List<Integer> heldSeats = new ArrayList<>();
        for (ShowtimeSeat seat : seats) {
            Optional<SeatHold> existingHold = seatHoldRepository
                    .findTop1ByShowtimeSeat_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                            seat.getId(), SeatHoldStatus.HELD, now);
            if (existingHold.isPresent()) {
                SeatHold hold = existingHold.get();
                if (hold.getUser() == null || !Objects.equals(hold.getUser().getId(), staffUser.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ghế đang được giữ bởi người khác");
                }
                hold.setExpiresAt(expiresAt);
                hold.setCreatedAt(now);
                seatHoldRepository.save(hold);
            } else {
                SeatHold newHold = SeatHold.builder()
                        .showtimeSeat(seat)
                        .user(staffUser)
                        .status(SeatHoldStatus.HELD)
                        .holdToken(UUID.randomUUID())
                        .createdAt(now)
                        .expiresAt(expiresAt)
                        .build();
                seatHoldRepository.save(newHold);
            }
            heldSeats.add(seat.getId());
        }
        return heldSeats;
    }

    @Override
    public void releaseSeatHolds(Integer showtimeId, List<Integer> seatIds, UserAccount staffUser) {
        if (staffUser == null) {
            return;
        }
        List<Integer> requestedSeats = sanitizeSeatSelections(seatIds);
        if (requestedSeats.isEmpty()) {
            return;
        }
        List<SeatHold> holds = seatHoldRepository.findByShowtimeSeat_IdInAndStatusAndUser(
                requestedSeats, SeatHoldStatus.HELD, staffUser);
        if (holds.isEmpty()) {
            return;
        }
        holds.forEach(hold -> hold.setStatus(SeatHoldStatus.RELEASED));
        seatHoldRepository.saveAll(holds);
    }

    @Override
    public StaffBookingResult createBookingForStaff(StaffBookingRequest request, UserAccount staffUser) {
        if (request.getShowtimeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Showtime is required");
        }
        List<Integer> requestedSeats = sanitizeSeatSelections(request.getSeatIds());
        if (requestedSeats.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ghế");
        }
        Showtime showtime = requireShowtime(request.getShowtimeId());
        List<ShowtimeSeat> seats = fetchShowtimeSeats(showtime.getId(), requestedSeats);
        ensureSeatsAvailable(seats, staffUser);

        Map<Integer, Combo> comboLookup = loadCombos();
        List<StaffComboSelection> comboSelections = sanitizeComboSelections(request.getComboSelections());
        List<BookingCombo> comboEntities = buildComboEntities(comboSelections, comboLookup);

        BigDecimal seatTotal = seats.stream()
                .map(ShowtimeSeat::getEffectivePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal comboTotal = comboEntities.stream()
                .map(BookingCombo::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal finalAmount = seatTotal.add(comboTotal).setScale(2, RoundingMode.HALF_UP);
        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị đơn hàng không hợp lệ");
        }

        OffsetDateTime now = currentTime();
        StaffPaymentMethod paymentMethod = request.getPaymentMethod() != null
                ? request.getPaymentMethod()
                : StaffPaymentMethod.CASH;

        Booking booking = new Booking();
        booking.setBookingCode(BookingCodeGenerator.newCode(now));
        booking.setShowtime(showtime);
        booking.setStaff(staffUser);
        booking.setBookingStatus(paymentMethod == StaffPaymentMethod.CASH ? BOOKING_STATUS_CONFIRMED : BOOKING_STATUS_PENDING);
        booking.setPaymentStatus(paymentMethod == StaffPaymentMethod.CASH ? PAYMENT_STATUS_PAID : PAYMENT_STATUS_UNPAID);
        booking.setPaymentMethod(paymentMethod.name());
        booking.setCustomerEmail(trimToNull(request.getCustomerEmail()));
        booking.setCustomerPhone(trimToNull(request.getCustomerPhone()));
        booking.setTotalAmount(finalAmount);
        booking.setFinalAmount(finalAmount);
        booking.setCreatedAt(now);
        booking.setPaidAt(paymentMethod == StaffPaymentMethod.CASH ? now : null);

        List<BookingSeat> seatEntities = new ArrayList<>();
        for (ShowtimeSeat seat : seats) {
            BookingSeat bookingSeat = BookingSeat.builder()
                    .booking(booking)
                    .showtimeSeat(seat)
                    .unitPrice(seat.getEffectivePrice())
                    .discountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .finalPrice(seat.getEffectivePrice())
                    .build();
            seatEntities.add(bookingSeat);
            seat.setStatus(STATUS_DISABLED);
        }
        booking.getBookingSeats().addAll(seatEntities);
        comboEntities.forEach(combo -> {
            combo.setBooking(booking);
            booking.getBookingCombos().add(combo);
        });

        if (paymentMethod == StaffPaymentMethod.VIETQR) {
            booking.getPaymentLogs().add(PaymentLog.builder()
                    .booking(booking)
                    .provider(PROVIDER_VIETQR)
                    .status("Init")
                    .amount(finalAmount)
                    .createdAt(now)
                    .rawMessage("Awaiting VietQR transfer")
                    .build());
        } else {
            booking.getPaymentLogs().add(PaymentLog.builder()
                    .booking(booking)
                    .provider(PROVIDER_CASH)
                    .status("Success")
                    .amount(finalAmount)
                    .createdAt(now)
                    .rawMessage("Cash collection")
                    .build());
        }

        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Một hoặc nhiều ghế đã được đặt trước");
        }
        showtimeSeatRepository.saveAll(seats);
        markSeatHoldsAsBooked(seats, staffUser);

        return StaffBookingResult.builder()
                .bookingCode(booking.getBookingCode())
                .paymentMethod(paymentMethod)
                .totalAmount(finalAmount)
                .finalAmount(finalAmount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffBookingDetailView getBookingDetail(String bookingCode) {
        Booking booking = bookingRepository.findDetailedByBookingCode(bookingCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn " + bookingCode));
        List<StaffBookingSeatDto> seats = booking.getBookingSeats().stream()
                .sorted(Comparator.comparing((BookingSeat bs) -> bs.getShowtimeSeat().getSeat().getRowLabel())
                        .thenComparing(bs -> bs.getShowtimeSeat().getSeat().getSeatNumber()))
                .map(bs -> StaffBookingSeatDto.builder()
                        .rowLabel(bs.getShowtimeSeat().getSeat().getRowLabel())
                        .seatNumber(bs.getShowtimeSeat().getSeat().getSeatNumber())
                        .price(bs.getFinalPrice())
                        .build())
                .toList();

        List<StaffBookingComboDto> combos = booking.getBookingCombos().stream()
                .sorted(Comparator.comparing(bc -> bc.getCombo().getName(), String.CASE_INSENSITIVE_ORDER))
                .map(bc -> StaffBookingComboDto.builder()
                        .name(bc.getCombo().getName())
                        .quantity(bc.getQuantity())
                        .totalPrice(bc.getTotalPrice())
                        .build())
                .toList();

        boolean isUnpaid = PAYMENT_STATUS_UNPAID.equalsIgnoreCase(booking.getPaymentStatus());
        boolean isPending = BOOKING_STATUS_PENDING.equalsIgnoreCase(booking.getBookingStatus());
        boolean isVietQr = StaffPaymentMethod.VIETQR.name().equalsIgnoreCase(booking.getPaymentMethod());

        return StaffBookingDetailView.builder()
                .bookingCode(booking.getBookingCode())
                .movieTitle(booking.getShowtime().getMovie().getTitle())
                .auditoriumName(booking.getShowtime().getAuditorium().getName())
                .showtimeStart(booking.getShowtime().getStartTime())
                .seats(seats)
                .combos(combos)
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .paymentMethod(booking.getPaymentMethod())
                .totalAmount(booking.getTotalAmount())
                .finalAmount(booking.getFinalAmount())
                .customerPhone(booking.getCustomerPhone())
                .customerEmail(booking.getCustomerEmail())
                .createdAt(booking.getCreatedAt())
                .paidAt(booking.getPaidAt())
                .cancelledAt(booking.getCancelledAt())
                .staffName(booking.getStaff() != null ? booking.getStaff().getFullName() : "N/A")
                .canMarkPaid(isUnpaid && !isVietQr)
                .canCancel(isPending && isUnpaid)
                .showQrButton(isUnpaid && isVietQr && isPending)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffVietQrInfo getVietQrInfo(String bookingCode) {
        Booking booking = bookingRepository.findDetailedByBookingCode(bookingCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn " + bookingCode));
        if (!StaffPaymentMethod.VIETQR.name().equalsIgnoreCase(booking.getPaymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn này không sử dụng VietQR");
        }
        if (PAYMENT_STATUS_PAID.equalsIgnoreCase(booking.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đã được thanh toán");
        }
        String transferContent = "CB_" + booking.getBookingCode();
        String qrUrl = buildVietQrUrl(booking.getFinalAmount(), transferContent);
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(QR_TIMEOUT_MINUTES);
        return StaffVietQrInfo.builder()
                .bookingCode(booking.getBookingCode())
                .amount(booking.getFinalAmount())
                .transferContent(transferContent)
                .qrImageUrl(qrUrl)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public void markBookingPaid(String bookingCode, UserAccount staffUser) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn " + bookingCode));
        if (PAYMENT_STATUS_PAID.equalsIgnoreCase(booking.getPaymentStatus())) {
            return;
        }
        if (BOOKING_STATUS_CANCELLED.equalsIgnoreCase(booking.getBookingStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đã bị huỷ");
        }
        if (staffUser != null) {
            booking.setStaff(staffUser);
        }
        OffsetDateTime now = currentTime();
        booking.setPaymentStatus(PAYMENT_STATUS_PAID);
        booking.setBookingStatus(BOOKING_STATUS_CONFIRMED);
        booking.setPaidAt(now);
        bookingRepository.save(booking);
        paymentLogRepository.findTopByBookingOrderByCreatedAtDesc(booking).ifPresent(log -> {
            log.setStatus("Success");
            log.setCreatedAt(now);
            paymentLogRepository.save(log);
        });
    }

    @Override
    public void cancelBooking(String bookingCode, UserAccount staffUser) {
        Booking booking = bookingRepository.findDetailedByBookingCode(bookingCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn " + bookingCode));
        if (BOOKING_STATUS_CANCELLED.equalsIgnoreCase(booking.getBookingStatus())) {
            return;
        }
        if (!PAYMENT_STATUS_UNPAID.equalsIgnoreCase(booking.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể huỷ đơn đã thanh toán");
        }
        OffsetDateTime now = currentTime();
        List<ShowtimeSeat> seatsToRelease = booking.getBookingSeats().stream()
                .map(BookingSeat::getShowtimeSeat)
                .toList();
        seatsToRelease.forEach(seat -> seat.setStatus(STATUS_AVAILABLE));
        showtimeSeatRepository.saveAll(seatsToRelease);
        booking.getBookingSeats().clear();
        booking.getBookingCombos().clear();
        booking.setBookingStatus(BOOKING_STATUS_CANCELLED);
        booking.setPaymentStatus(PAYMENT_STATUS_UNPAID);
        booking.setCancelledAt(now);
        if (staffUser != null) {
            booking.setStaff(staffUser);
        }
        bookingRepository.save(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffBookingStatusResponse getBookingStatus(String bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn " + bookingCode));
        return StaffBookingStatusResponse.builder()
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .build();
    }

    private Showtime requireShowtime(Integer id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy suất chiếu " + id));
    }

    private List<ShowtimeSeat> fetchShowtimeSeats(Integer showtimeId, List<Integer> seatIds) {
        List<ShowtimeSeat> seats = showtimeSeatRepository.findAllById(seatIds);
        Map<Integer, ShowtimeSeat> byId = seats.stream()
                .collect(Collectors.toMap(ShowtimeSeat::getId, s -> s));
        List<ShowtimeSeat> ordered = new ArrayList<>();
        for (Integer seatId : seatIds) {
            ShowtimeSeat seat = byId.get(seatId);
            if (seat == null || !Objects.equals(seat.getShowtime().getId(), showtimeId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ghế không hợp lệ cho suất chiếu");
            }
            ordered.add(seat);
        }
        return ordered;
    }

    private void ensureSeatsAvailable(List<ShowtimeSeat> seats, UserAccount staffUser) {
        if (seats.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ghế");
        }
        Set<Integer> seatIds = seats.stream().map(ShowtimeSeat::getId).collect(Collectors.toSet());
        if (!seatIds.isEmpty()) {
            long lockedSeats = bookingSeatRepository.countActiveSeatsForShowtimeSeats(seatIds);
            if (lockedSeats > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Một số ghế đã có người đặt");
            }
        }
        boolean hasDisabled = seats.stream().anyMatch(seat -> STATUS_DISABLED.equalsIgnoreCase(seat.getStatus()));
        if (hasDisabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có ghế không thể bán");
        }
        OffsetDateTime now = currentTime();
        for (ShowtimeSeat seat : seats) {
            Optional<SeatHold> activeHold = seatHoldRepository
                    .findTop1ByShowtimeSeat_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                            seat.getId(), SeatHoldStatus.HELD, now);
            if (activeHold.isPresent()) {
                UserAccount holder = activeHold.get().getUser();
                if (holder == null || staffUser == null || !Objects.equals(holder.getId(), staffUser.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Một số ghế đang được giữ bởi người khác");
                }
            }
        }
    }

    private Map<Integer, Combo> loadCombos() {
        return comboRepository.findAll().stream()
                .collect(Collectors.toMap(Combo::getId, combo -> combo));
    }

    private List<StaffComboSelection> sanitizeComboSelections(List<StaffComboSelection> selections) {
        if (selections == null) {
            return List.of();
        }
        return selections.stream()
                .filter(selection -> selection.getComboId() != null && selection.getQuantity() != null && selection.getQuantity() > 0)
                .toList();
    }

    private List<BookingCombo> buildComboEntities(List<StaffComboSelection> selections, Map<Integer, Combo> comboLookup) {
        Map<Integer, Integer> aggregated = new LinkedHashMap<>();
        for (StaffComboSelection selection : selections) {
            aggregated.merge(selection.getComboId(), selection.getQuantity(), Integer::sum);
        }
        List<BookingCombo> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : aggregated.entrySet()) {
            Combo combo = comboLookup.get(entry.getKey());
            if (combo == null || !Boolean.TRUE.equals(combo.getActive())) {
                continue;
            }
            BigDecimal quantity = BigDecimal.valueOf(entry.getValue());
            BigDecimal total = combo.getPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            result.add(BookingCombo.builder()
                    .combo(combo)
                    .quantity(entry.getValue())
                    .unitPrice(combo.getPrice())
                    .totalPrice(total)
                    .build());
        }
        return result;
    }

    private List<Integer> sanitizeSeatSelections(List<Integer> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(seatIds));
    }

    private void markSeatHoldsAsBooked(List<ShowtimeSeat> seats, UserAccount staffUser) {
        if (staffUser == null || seats.isEmpty()) {
            return;
        }
        List<Integer> seatIds = seats.stream().map(ShowtimeSeat::getId).toList();
        List<SeatHold> holds = seatHoldRepository.findByShowtimeSeat_IdInAndStatusAndUser(
                seatIds, SeatHoldStatus.HELD, staffUser);
        if (holds.isEmpty()) {
            return;
        }
        holds.forEach(hold -> hold.setStatus(SeatHoldStatus.BOOKED));
        seatHoldRepository.saveAll(holds);
    }

    private String resolveSeatStatus(ShowtimeSeat seat, Set<Integer> bookedSeatIds, Set<Integer> heldSeatIds) {
        if (bookedSeatIds.contains(seat.getId())) {
            return STATUS_BOOKED.toUpperCase(Locale.ROOT);
        }
        if (heldSeatIds.contains(seat.getId())) {
            return "HELD";
        }
        if (!STATUS_AVAILABLE.equalsIgnoreCase(seat.getStatus())) {
            return STATUS_DISABLED.toUpperCase(Locale.ROOT);
        }
        return STATUS_AVAILABLE.toUpperCase(Locale.ROOT);
    }

    private OffsetDateTime currentTime() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String buildVietQrUrl(BigDecimal amount, String content) {
        String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
        return "https://img.vietqr.io/image/"
                + VIETQR_BANK_CODE + "-"
                + VIETQR_ACCOUNT + "-compact.png?amount="
                + amount.setScale(0, RoundingMode.HALF_UP)
                + "&addInfo=" + encodedContent
                + "&accountName=" + URLEncoder.encode(VIETQR_ACCOUNT_NAME, StandardCharsets.UTF_8);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
