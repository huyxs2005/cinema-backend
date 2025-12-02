package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.CancelBookingResponse;
import com.cinema.hub.backend.dto.CreateBookingRequest;
import com.cinema.hub.backend.dto.CreateBookingResponse;
import com.cinema.hub.backend.dto.SeatHoldRequest;
import com.cinema.hub.backend.dto.SeatHoldResponse;
import com.cinema.hub.backend.dto.SeatMapItemDto;
import com.cinema.hub.backend.dto.SeatStatusRow;
import com.cinema.hub.backend.dto.SeatBookingBlock;
import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.BookingSeat;
import com.cinema.hub.backend.entity.Seat;
import com.cinema.hub.backend.entity.SeatHold;
import com.cinema.hub.backend.entity.Showtime;
import com.cinema.hub.backend.entity.ShowtimeSeat;
import com.cinema.hub.backend.entity.Ticket;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import com.cinema.hub.backend.entity.enums.SeatHoldStatus;
import com.cinema.hub.backend.entity.enums.ShowtimeSeatStatus;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.repository.BookingSeatRepository;
import com.cinema.hub.backend.repository.SeatHoldRepository;
import com.cinema.hub.backend.repository.SeatRepository;
import com.cinema.hub.backend.repository.ShowtimeRepository;
import com.cinema.hub.backend.repository.ShowtimeSeatRepository;
import com.cinema.hub.backend.repository.TicketRepository;
import com.cinema.hub.backend.repository.UserAccountRepository;
import com.cinema.hub.backend.service.exception.SeatSelectionException;
import com.cinema.hub.backend.util.TimeProvider;
import com.cinema.hub.backend.web.view.CheckoutPageView;
import com.cinema.hub.backend.web.view.SeatSelectionItemView;
import com.cinema.hub.backend.web.view.SeatSelectionShowtimeView;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatReservationService {

    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final UserAccountRepository userAccountRepository;
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    public List<SeatMapItemDto> getSeatMap(int showtimeId) {
        showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new EntityNotFoundException("Showtime not found: " + showtimeId));
        OffsetDateTime now = TimeProvider.now();
        List<SeatStatusRow> rows = showtimeSeatRepository.fetchSeatStatusRows(showtimeId, now);
        Set<Integer> disabledSeatIds = loadDisabledSeatIds();
        return rows.stream()
                .map(row -> toSeatMapItem(row, disabledSeatIds))
                .toList();
    }

    @Transactional(readOnly = true)
    public CheckoutPageView getCheckoutView(int showtimeId, String holdTokenValue, Integer userId) {
        UUID token = parseHoldToken(holdTokenValue);
        OffsetDateTime now = TimeProvider.now();
        List<SeatHold> holds = seatHoldRepository.findActiveHoldsByToken(token, now);
        if (holds.isEmpty()) {
            throw new SeatSelectionException("Phiên giữ ghế đã hết hạn hoặc không tồn tại.");
        }
        validateHoldOwnership(holds, userId);
        SeatHold firstHold = holds.get(0);
        Integer holdShowtimeId = firstHold.getShowtimeSeat().getShowtime().getId();
        if (!holdShowtimeId.equals(showtimeId)) {
            throw new SeatSelectionException("Hold token không khớp với suất chiếu đã chọn.");
        }
        SeatSelectionShowtimeView showtimeView = SeatSelectionShowtimeView.fromEntity(firstHold.getShowtimeSeat().getShowtime());
        List<SeatSelectionItemView> seats = holds.stream()
                .map(this::toSeatSelectionItem)
                .sorted(Comparator.comparing(SeatSelectionItemView::getLabel))
                .toList();
        List<Integer> seatIds = holds.stream()
                .map(hold -> hold.getShowtimeSeat().getSeat().getId())
                .toList();
        BigDecimal total = seats.stream()
                .map(seat -> seat.getPrice() != null ? seat.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CheckoutPageView.builder()
                .holdToken(firstHold.getHoldToken().toString())
                .expiresAt(firstHold.getExpiresAt())
                .bookingId(null)
                .bookingCode(null)
                .showtime(showtimeView)
                .seats(seats)
                .seatIds(seatIds)
                .total(total)
                .seatCount(seats.size())
                .build();
    }

    @Transactional(readOnly = true)
    public CheckoutPageView getCheckoutViewForBooking(int bookingId, Integer userId) {
        Booking booking = bookingRepository.findDetailedById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (booking.getUser() == null || userId == null || !booking.getUser().getId().equals(userId)) {
            throw new SeatSelectionException("Phiên thanh toán không thuộc về tài khoản hiện tại.");
        }
        List<BookingSeat> bookingSeats = bookingSeatRepository.findDetailedByBooking(bookingId);
        if (bookingSeats.isEmpty()) {
            throw new SeatSelectionException("Đơn đặt vé không chứa ghế hợp lệ.");
        }
        List<SeatSelectionItemView> seats = bookingSeats.stream()
                .map(bs -> {
                    var seat = bs.getShowtimeSeat().getSeat();
                    String label = seat.getRowLabel() + seat.getSeatNumber();
                    String seatTypeName = seat.getSeatType() != null ? seat.getSeatType().getName() : null;
                    return SeatSelectionItemView.builder()
                            .label(label)
                            .price(bs.getFinalPrice())
                            .seatType(seatTypeName)
                            .build();
                })
                .sorted(Comparator.comparing(SeatSelectionItemView::getLabel))
                .toList();
        List<Integer> seatIds = bookingSeats.stream()
                .map(bs -> bs.getShowtimeSeat().getSeat().getId())
                .toList();
        BigDecimal total = booking.getFinalAmount() != null && booking.getFinalAmount().signum() > 0
                ? booking.getFinalAmount()
                : booking.getTotalAmount();
        return CheckoutPageView.builder()
                .holdToken(null)
                .expiresAt(booking.getCreatedAt())
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .showtime(SeatSelectionShowtimeView.fromEntity(booking.getShowtime()))
                .seats(seats)
                .seatIds(seatIds)
                .total(total)
                .seatCount(seats.size())
                .build();
    }

    @Transactional
    public SeatHoldResponse holdSeats(SeatHoldRequest request) {
        if (CollectionUtils.isEmpty(request.getSeatIds())) {
            throw new SeatSelectionException("Seat list cannot be empty");
        }
        Set<Integer> uniqueSeatIds = new HashSet<>(request.getSeatIds());
        Set<Integer> expandedSeatIds = expandSeatIdsForCouples(uniqueSeatIds);
        OffsetDateTime now = TimeProvider.now();

        if (StringUtils.hasText(request.getPreviousHoldToken())) {
            releaseHoldToken(parseHoldToken(request.getPreviousHoldToken()));
        }

        List<SeatHold> newHolds = prepareSeatHolds(request.getShowtimeId(), expandedSeatIds, request.getUserId(), now);

        seatHoldRepository.saveAll(newHolds);

        UUID token = newHolds.get(0).getHoldToken();
        OffsetDateTime expiresAt = newHolds.get(0).getExpiresAt();
        return new SeatHoldResponse(token.toString(), expiresAt);
    }

    @Transactional
    public CreateBookingResponse createBooking(CreateBookingRequest request) {
        UUID token = parseHoldToken(request.getHoldToken());
        OffsetDateTime now = TimeProvider.now();

        List<SeatHold> holds = seatHoldRepository.findActiveHoldsByTokenForUpdate(token, now);
        if (holds.isEmpty()) {
            throw new SeatSelectionException("Hold token is invalid or expired");
        }

        validateHoldOwnership(holds, request.getUserId());
        UserAccount user = loadUser(request.getUserId());
        Set<Integer> disabledSeatIds = loadDisabledSeatIds();
        ensureHoldSeatsEnabled(holds, disabledSeatIds);

        ensureShowtimeAcceptsBookings(holds.get(0).getShowtimeSeat().getShowtime(), now);

        Booking booking = buildBookingFromHolds(holds, user, request.getPaymentMethod(), now);
        booking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = persistBookingSeats(booking, holds);
        persistTickets(booking, bookingSeats, now);
        releaseHoldToken(token);
        if (user != null && user.getId() != null) {
            seatHoldRepository.releaseByUserId(user.getId());
        }

        return new CreateBookingResponse(booking.getId(), booking.getBookingCode());
    }

    @Transactional
    public CancelBookingResponse cancelBooking(int bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        OffsetDateTime now = TimeProvider.now();
        OffsetDateTime showtimeStart = booking.getShowtime().getStartTime().atOffset(TimeProvider.VN_ZONE_OFFSET);
        if (!showtimeStart.isAfter(now)) {
            throw new SeatSelectionException("Showtime already started. Booking cannot be cancelled.");
        }

        booking.setBookingStatus(BookingStatus.Cancelled);
        booking.setCancelledAt(now);

        boolean refundTriggered = false;
        if (booking.getPaymentStatus() == PaymentStatus.Paid) {
            refundTriggered = true;
            log.info("Refund triggered for booking {}", booking.getBookingCode());
        }

        ticketRepository.deleteByBookingSeat_Booking_Id(bookingId);
        bookingSeatRepository.deleteByBookingId(bookingId);

        return new CancelBookingResponse(booking.getId(), booking.getBookingStatus(), booking.getPaymentStatus(), refundTriggered);
    }

    @Transactional
    public int expireSeatHolds() {
        OffsetDateTime now = TimeProvider.now();
        return seatHoldRepository.expireStaleHolds(now);
    }

    @Transactional
    public void releaseHoldsForUser(Integer userId) {
        if (userId == null) {
            return;
        }
        seatHoldRepository.releaseByUserId(userId);
    }

    @Transactional
    public void releaseHold(String tokenValue) {
        UUID token = parseHoldToken(tokenValue);
        releaseHoldToken(token);
    }

    private List<SeatHold> prepareSeatHolds(int showtimeId,
                                            Set<Integer> seatIds,
                                            Integer userId,
                                            OffsetDateTime now) {
        if (seatIds.isEmpty()) {
            throw new SeatSelectionException("Seat list cannot be empty");
        }

        List<com.cinema.hub.backend.entity.ShowtimeSeat> lockedSeats =
                showtimeSeatRepository.lockSeatsForHold(showtimeId, seatIds);

        if (lockedSeats.isEmpty()) {
            throw new SeatSelectionException("Không tìm thấy ghế hợp lệ cho suất chiếu.");
        }
        ensureShowtimeAcceptsBookings(lockedSeats.get(0).getShowtime(), now);

        if (lockedSeats.size() != seatIds.size()) {
            throw new SeatSelectionException("One or more seats do not belong to the showtime");
        }

        Set<Integer> disabledSeatIds = loadDisabledSeatIds();
        ensureSeatsEnabled(lockedSeats, disabledSeatIds);

        Set<Integer> showtimeSeatIds = lockedSeats.stream()
                .map(com.cinema.hub.backend.entity.ShowtimeSeat::getId)
                .collect(Collectors.toSet());

        ticketRepository.deleteCancelledTicketsByShowtimeSeatIds(showtimeSeatIds);
        bookingSeatRepository.deleteCancelledSeatsByShowtimeSeatIds(showtimeSeatIds);

        detectActiveHolds(showtimeSeatIds, now, userId);
        detectActiveBookings(showtimeSeatIds, userId);

        UserAccount user = userId != null ? loadUser(userId) : null;

        UUID token = UUID.randomUUID();
        OffsetDateTime expiresAt = now.plusMinutes(10);
        List<SeatHold> holds = new ArrayList<>();
        for (com.cinema.hub.backend.entity.ShowtimeSeat seat : lockedSeats) {
            String status = seat.getStatus();
            if (status != null && !"AVAILABLE".equalsIgnoreCase(status)) {
                throw new SeatSelectionException("Seat " + seat.getSeat().getId() + " is not sellable");
            }
            holds.add(SeatHold.builder()
                    .showtimeSeat(seat)
                    .holdToken(token)
                    .user(user)
                    .createdAt(now)
                    .expiresAt(expiresAt)
                    .status(SeatHoldStatus.Held)
                    .build());
        }
        return holds;
    }

    private void detectActiveBookings(Set<Integer> showtimeSeatIds, Integer userId) {
        if (showtimeSeatIds.isEmpty()) {
            return;
        }
        List<SeatBookingBlock> blockingBookings = bookingSeatRepository.findBlockingBookings(showtimeSeatIds);
        if (blockingBookings.isEmpty()) {
            return;
        }
        Set<Integer> releasable = new HashSet<>();
        Set<Integer> blockingSeatIds = new HashSet<>();
        for (com.cinema.hub.backend.dto.SeatBookingBlock block : blockingBookings) {
            if (block.paymentStatus() == PaymentStatus.Paid) {
                blockingSeatIds.add(block.seatId());
                continue;
            }
            if (userId != null && block.bookingUserId() != null && block.bookingUserId().equals(userId)) {
                releasable.add(block.bookingId());
                continue;
            }
            blockingSeatIds.add(block.seatId());
        }
        if (!releasable.isEmpty()) {
            cancelUserBookings(releasable);
        }
        if (!blockingSeatIds.isEmpty()) {
            throw new SeatSelectionException("Seat already held");
        }
    }

    private void detectActiveHolds(Set<Integer> showtimeSeatIds, OffsetDateTime now, Integer userId) {
        List<SeatHold> activeHolds = seatHoldRepository.findActiveHoldsBySeatIds(showtimeSeatIds, now);
        if (activeHolds.isEmpty()) {
            return;
        }
        Set<UUID> releasableTokens = new HashSet<>();
        for (SeatHold hold : activeHolds) {
            Integer holderId = hold.getUser() != null ? hold.getUser().getId() : null;
            if (holderId != null && userId != null && holderId.equals(userId)) {
                releasableTokens.add(hold.getHoldToken());
                continue;
            }
            throw new SeatSelectionException("Seat already held");
        }
        releasableTokens.forEach(seatHoldRepository::releaseByToken);
    }

    private void validateHoldOwnership(List<SeatHold> holds, Integer userId) {
        boolean anyOtherUser = holds.stream()
                .map(SeatHold::getUser)
                .filter(user -> user != null && userId != null)
                .anyMatch(user -> !user.getId().equals(userId));
        if (anyOtherUser) {
            throw new SeatSelectionException("Hold token does not belong to the requesting user");
        }
    }

    private UserAccount loadUser(Integer userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private Booking buildBookingFromHolds(List<SeatHold> holds,
                                          UserAccount user,
                                          String paymentMethod,
                                          OffsetDateTime now) {
        BigDecimal total = holds.stream()
                .map(hold -> hold.getShowtimeSeat().getEffectivePrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Booking.builder()
                .bookingCode(generateBookingCode())
                .user(user)
                .showtime(holds.get(0).getShowtimeSeat().getShowtime())
                .bookingStatus(BookingStatus.Pending)
                .paymentStatus(PaymentStatus.Unpaid)
                .paymentMethod(paymentMethod)
                .totalAmount(total)
                .finalAmount(total)
                .createdAt(now)
                .build();
    }

    private List<BookingSeat> persistBookingSeats(Booking booking, List<SeatHold> holds) {
        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (SeatHold hold : holds) {
            bookingSeats.add(BookingSeat.builder()
                    .booking(booking)
                    .showtimeSeat(hold.getShowtimeSeat())
                    .unitPrice(hold.getShowtimeSeat().getEffectivePrice())
                    .discountAmount(BigDecimal.ZERO)
                    .finalPrice(hold.getShowtimeSeat().getEffectivePrice())
                    .seatTag(null)
                    .appliedPromotionCode(null)
                    .appliedPromotionNote(null)
                    .build());
        }
        return bookingSeatRepository.saveAll(bookingSeats);
    }

    private void persistTickets(Booking booking, List<BookingSeat> bookingSeats, OffsetDateTime now) {
        List<Ticket> tickets = new ArrayList<>();
        for (BookingSeat bookingSeat : bookingSeats) {
            String ticketCode = "TKT-" + booking.getBookingCode() + "-" + bookingSeat.getShowtimeSeat().getSeat().getId();
            tickets.add(Ticket.builder()
                    .bookingSeat(bookingSeat)
                    .ticketCode(ticketCode)
                    .qrCodeData("BOOKING:" + booking.getBookingCode() + ";SEAT:" +
                            bookingSeat.getShowtimeSeat().getSeat().getRowLabel() + bookingSeat.getShowtimeSeat().getSeat().getSeatNumber())
                    .issuedAt(now)
                    .build());
        }
        ticketRepository.saveAll(tickets);
    }

    private SeatSelectionItemView toSeatSelectionItem(SeatHold hold) {
        var seat = hold.getShowtimeSeat().getSeat();
        String label = seat.getRowLabel() + seat.getSeatNumber();
        String seatTypeName = seat.getSeatType() != null ? seat.getSeatType().getName() : null;
        return SeatSelectionItemView.builder()
                .label(label)
                .price(hold.getShowtimeSeat().getEffectivePrice())
                .seatType(seatTypeName)
                .build();
    }

    private SeatMapItemDto toSeatMapItem(SeatStatusRow row, Set<Integer> disabledSeatIds) {
        String status = resolveSeatStatus(row, disabledSeatIds);
        boolean selectable = "AVAILABLE".equals(status);
        String seatLabel = row.rowLabel() + row.seatNumber();
        Integer ownerId = row.holdUserId() != null ? row.holdUserId() : row.bookingUserId();
        return SeatMapItemDto.builder()
                .seatId(row.seatId())
                .rowLabel(row.rowLabel())
                .seatNumber(row.seatNumber())
                .seatLabel(seatLabel)
                .seatType(row.seatTypeName())
                .coupleGroupId(resolveCoupleGroupId(row))
                .price(row.effectivePrice())
                .status(status)
                .selectable(selectable)
                .holdUserId(ownerId)
                .build();
    }

    private String resolveCoupleGroupId(SeatStatusRow row) {
        if (row.coupleGroupId() != null && !row.coupleGroupId().isBlank()) {
            return row.coupleGroupId();
        }
        if ("Couple".equalsIgnoreCase(row.seatTypeName())
                && row.rowLabel() != null
                && row.seatNumber() != null) {
            int baseSeatNumber = row.seatNumber() % 2 == 0 ? row.seatNumber() - 1 : row.seatNumber();
            return row.rowLabel() + "-" + baseSeatNumber;
        }
        return null;
    }

    private String resolveSeatStatus(SeatStatusRow row, Set<Integer> disabledSeatIds) {
        if (disabledSeatIds.contains(row.seatId())) {
            return "DISABLED";
        }
        if (row.holdStatus() != null) {
            return "HELD";
        }
        if (row.bookingStatus() != null) {
            return row.paymentStatus() == PaymentStatus.Paid ? "SOLD" : "HELD";
        }
        return "AVAILABLE".equalsIgnoreCase(row.seatStatus()) ? "AVAILABLE" : "DISABLED";
    }

    private Set<Integer> expandSeatIdsForCouples(Set<Integer> requestedSeatIds) {
        if (requestedSeatIds.isEmpty()) {
            return requestedSeatIds;
        }
        List<Seat> seats = seatRepository.findAllById(requestedSeatIds);
        if (seats.size() != requestedSeatIds.size()) {
            throw new SeatSelectionException("One or more seats do not exist");
        }
        Set<Integer> expandedIds = new HashSet<>(requestedSeatIds);
        Set<String> processedGroups = new HashSet<>();
        for (Seat seat : seats) {
            if (!isCoupleSeat(seat)) {
                continue;
            }
            String groupId = seat.getResolvedCoupleGroupId();
            if (groupId == null || !processedGroups.add(groupId)) {
                continue;
            }
            Integer auditoriumId = seat.getAuditorium() != null ? seat.getAuditorium().getId() : null;
            if (auditoriumId == null || seat.getSeatNumber() == null || seat.getRowLabel() == null) {
                continue;
            }
            int baseSeatNumber = seat.getSeatNumber() % 2 == 0 ? seat.getSeatNumber() - 1 : seat.getSeatNumber();
            seatRepository.findByAuditorium_IdAndRowLabelIgnoreCaseAndSeatNumber(
                            auditoriumId, seat.getRowLabel(), baseSeatNumber)
                    .ifPresent(partner -> expandedIds.add(partner.getId()));
            seatRepository.findByAuditorium_IdAndRowLabelIgnoreCaseAndSeatNumber(
                            auditoriumId, seat.getRowLabel(), baseSeatNumber + 1)
                    .ifPresent(partner -> expandedIds.add(partner.getId()));
        }
        return expandedIds;
    }

    private boolean isCoupleSeat(Seat seat) {
        return seat.getSeatType() != null
                && seat.getSeatType().getName() != null
                && "Couple".equalsIgnoreCase(seat.getSeatType().getName());
    }

    private Set<Integer> loadDisabledSeatIds() {
        return Set.of();
    }

    private void ensureSeatsEnabled(List<ShowtimeSeat> seats, Set<Integer> disabledSeatIds) {
        if (disabledSeatIds.isEmpty()) {
            return;
        }
        for (ShowtimeSeat showtimeSeat : seats) {
            if (disabledSeatIds.contains(showtimeSeat.getSeat().getId())) {
                throw new SeatSelectionException("Seat disabled for this format");
            }
        }
    }

    private void ensureHoldSeatsEnabled(List<SeatHold> holds, Set<Integer> disabledSeatIds) {
        if (disabledSeatIds.isEmpty()) {
            return;
        }
        for (SeatHold hold : holds) {
            if (disabledSeatIds.contains(hold.getShowtimeSeat().getSeat().getId())) {
                throw new SeatSelectionException("Seat disabled for this format");
            }
        }
    }

    private UUID parseHoldToken(String tokenValue) {
        try {
            return UUID.fromString(tokenValue);
        } catch (IllegalArgumentException ex) {
            throw new SeatSelectionException("Hold token format is invalid");
        }
    }

    private void releaseHoldToken(UUID token) {
        seatHoldRepository.releaseByToken(token);
    }

    private String generateBookingCode() {
        String code;
        do {
            code = "BK-" + ThreadLocalRandom.current().nextInt(100000, 999999);
        } while (bookingRepository.existsByBookingCode(code));
        return code;
    }

    private void cancelUserBookings(Set<Integer> bookingIds) {
        if (bookingIds.isEmpty()) {
            return;
        }
        OffsetDateTime now = TimeProvider.now();
        for (Integer bookingId : bookingIds) {
            bookingRepository.findByIdForUpdate(bookingId).ifPresent(booking -> {
                if (booking.getBookingStatus() == BookingStatus.Cancelled
                        || booking.getPaymentStatus() == PaymentStatus.Paid) {
                    return;
                }
                booking.setBookingStatus(BookingStatus.Cancelled);
                booking.setPaymentStatus(PaymentStatus.Unpaid);
                booking.setCancelledAt(now);
                bookingRepository.save(booking);
                ticketRepository.deleteByBookingSeat_Booking_Id(bookingId);
                bookingSeatRepository.deleteByBookingId(bookingId);
                log.info("Released pending booking {} for user {}", booking.getBookingCode(),
                        booking.getUser() != null ? booking.getUser().getId() : null);
            });
        }
    }

    private void ensureShowtimeAcceptsBookings(Showtime showtime, OffsetDateTime now) {
        if (showtime == null || showtime.getStartTime() == null) {
            throw new SeatSelectionException("Không thể xác định suất chiếu để giữ ghế.");
        }
        OffsetDateTime start = showtime.getStartTime().atOffset(TimeProvider.VN_ZONE_OFFSET);
        if (!start.isAfter(now)) {
            throw new SeatSelectionException("Suất chiếu đã bắt đầu hoặc kết thúc. Không thể giữ hoặc đặt ghế.");
        }
    }
}
