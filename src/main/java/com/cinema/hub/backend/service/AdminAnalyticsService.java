package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.admin.analytics.AnalyticsSummaryRangeDto;
import com.cinema.hub.backend.dto.admin.analytics.OrderStatusBreakdownDto;
import com.cinema.hub.backend.dto.admin.analytics.PayOSTransactionDto;
import com.cinema.hub.backend.dto.admin.analytics.PaymentMethodBreakdownDto;
import com.cinema.hub.backend.dto.admin.analytics.RevenueTrendPointDto;
import com.cinema.hub.backend.entity.Booking;
import com.cinema.hub.backend.entity.enums.BookingStatus;
import com.cinema.hub.backend.entity.enums.PaymentStatus;
import com.cinema.hub.backend.repository.BookingRepository;
import com.cinema.hub.backend.util.TimeProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private static final String PAYMENT_UNKNOWN = "Unknown";
    private static final String PAYMENT_PAYOS = "PayOS";
    private static final String PAYMENT_CASH = "Cash";
    private static final String PAYMENT_BANK = "Bank Transfer";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final BookingRepository bookingRepository;
    public AnalyticsSummaryRangeDto getSummaryRange(LocalDate from, LocalDate to) {
        OffsetDateTime earliestRecord = bookingRepository.findEarliestCreatedAt();
        OffsetDateTime latestRecord = bookingRepository.findLatestCreatedAt();
        DateRange currentRange = resolveRange(from, to, earliestRecord, latestRecord);
        DateRange previousRange = currentRange.previous();

        List<Booking> currentBookings = bookingRepository
                .findByCreatedAtBetween(currentRange.start(), currentRange.end());
        List<Booking> previousBookings = bookingRepository
                .findByCreatedAtBetween(previousRange.start(), previousRange.end());

        SummaryMetrics currentMetrics = computeSummaryMetrics(currentBookings);
        SummaryMetrics previousMetrics = computeSummaryMetrics(previousBookings);

        Map<String, BigDecimal> revenueByMethod = sortRevenueMap(currentMetrics.revenueByPaymentMethod());

        BigDecimal growthPercent = calculateGrowthPercent(previousMetrics.totalRevenue(), currentMetrics.totalRevenue());

        return AnalyticsSummaryRangeDto.builder()
                .totalRevenue(currentMetrics.totalRevenue())
                .totalOrders(currentMetrics.totalOrders())
                .totalPaidOrders(currentMetrics.totalPaidOrders())
                .totalFailedOrders(currentMetrics.totalFailedOrders())
                .totalCancelledOrders(currentMetrics.totalCancelledOrders())
                .revenueByPaymentMethod(revenueByMethod)
                .previousPeriodRevenue(previousMetrics.totalRevenue())
                .revenueGrowthPercent(growthPercent)
                .createdFrom(currentRange.start())
                .createdTo(currentRange.end())
                .earliestRecord(earliestRecord)
                .latestRecord(latestRecord)
                .build();
    }

    public List<PaymentMethodBreakdownDto> getPaymentMethodBreakdown(LocalDate from, LocalDate to) {
        DateRange range = resolveRange(from, to,
                bookingRepository.findEarliestCreatedAt(),
                bookingRepository.findLatestCreatedAt());
        List<Booking> bookings = bookingRepository.findByCreatedAtBetween(range.start(), range.end());
        Map<String, PaymentMethodAccumulator> aggregates = new LinkedHashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Booking booking : bookings) {
            if (booking.getPaymentStatus() != PaymentStatus.Paid) {
                continue;
            }
            BigDecimal amount = safeAmount(booking);
            totalRevenue = totalRevenue.add(amount);
            String method = normalizePaymentMethod(booking.getPaymentMethod());
            PaymentMethodAccumulator accumulator = aggregates.computeIfAbsent(method, key -> new PaymentMethodAccumulator());
            accumulator.increment(amount);
        }

        final BigDecimal totalRevenueFinal = totalRevenue;
        BigDecimal denominator = totalRevenueFinal.compareTo(BigDecimal.ZERO) > 0 ? totalRevenueFinal : BigDecimal.ONE;

        return aggregates.entrySet().stream()
                .sorted(Map.Entry.<String, PaymentMethodAccumulator>comparingByValue(
                        Comparator.comparing(PaymentMethodAccumulator::getTotalAmount).reversed()))
                .map(entry -> PaymentMethodBreakdownDto.builder()
                        .method(entry.getKey())
                        .totalAmount(entry.getValue().getTotalAmount())
                        .count(entry.getValue().getCount())
                        .percentage(totalRevenueFinal.compareTo(BigDecimal.ZERO) > 0
                                ? entry.getValue().getTotalAmount()
                                        .multiply(ONE_HUNDRED)
                                        .divide(denominator, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toCollection(LinkedList::new));
    }

    public List<OrderStatusBreakdownDto> getStatusBreakdown(LocalDate from, LocalDate to) {
        DateRange range = resolveRange(from, to,
                bookingRepository.findEarliestCreatedAt(),
                bookingRepository.findLatestCreatedAt());
        List<Booking> bookings = bookingRepository.findByCreatedAtBetween(range.start(), range.end());
        Map<String, OrderStatusAccumulator> aggregates = new LinkedHashMap<>();
        long totalOrders = bookings.size();

        for (Booking booking : bookings) {
            String bookingStatus = booking.getBookingStatus() != null
                    ? booking.getBookingStatus().name()
                    : BookingStatus.Pending.name();
            String paymentStatus = booking.getPaymentStatus() != null
                    ? booking.getPaymentStatus().name()
                    : PaymentStatus.Unpaid.name();
            String key = bookingStatus + "|" + paymentStatus;
            OrderStatusAccumulator accumulator = aggregates.computeIfAbsent(key,
                    k -> new OrderStatusAccumulator(bookingStatus, paymentStatus));
            accumulator.increment(safeAmount(booking));
        }

        BigDecimal totalOrdersBigDecimal = totalOrders > 0
                ? BigDecimal.valueOf(totalOrders)
                : BigDecimal.ONE;

        return aggregates.values().stream()
                .sorted(Comparator.comparingLong(OrderStatusAccumulator::getCount).reversed())
                .map(acc -> OrderStatusBreakdownDto.builder()
                        .bookingStatus(acc.getBookingStatus())
                        .paymentStatus(acc.getPaymentStatus())
                        .count(acc.getCount())
                        .revenue(acc.getRevenue())
                        .percentage(totalOrders > 0
                                ? BigDecimal.valueOf(acc.getCount())
                                        .multiply(ONE_HUNDRED)
                                        .divide(totalOrdersBigDecimal, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    public List<RevenueTrendPointDto> getRevenueTrend(LocalDate from, LocalDate to) {
        DateRange range = resolveRange(from, to,
                bookingRepository.findEarliestCreatedAt(),
                bookingRepository.findLatestCreatedAt());
        List<Booking> bookings = bookingRepository.findByCreatedAtBetween(range.start(), range.end());

        Map<LocalDate, TrendAccumulator> dailyMap = new LinkedHashMap<>();
        LocalDate cursor = range.startDate();
        while (!cursor.isAfter(range.endDate())) {
            dailyMap.put(cursor, new TrendAccumulator(cursor));
            cursor = cursor.plusDays(1);
        }

        for (Booking booking : bookings) {
            LocalDate bookingDate = toLocalDate(booking.getCreatedAt());
            TrendAccumulator accumulator = dailyMap.get(bookingDate);
            if (accumulator == null) {
                accumulator = new TrendAccumulator(bookingDate);
                dailyMap.put(bookingDate, accumulator);
            }
            PaymentStatus paymentStatus = booking.getPaymentStatus();
            BookingStatus bookingStatus = booking.getBookingStatus();
            if (paymentStatus == PaymentStatus.Paid) {
                accumulator.addPaid(safeAmount(booking));
            } else if (paymentStatus == PaymentStatus.Failed) {
                accumulator.incrementFailed();
            }
            if (bookingStatus == BookingStatus.Cancelled) {
                accumulator.incrementCancelled();
            }
        }

        return dailyMap.values().stream()
                .map(acc -> RevenueTrendPointDto.builder()
                        .date(acc.getDate())
                        .paidRevenue(acc.getPaidRevenue())
                        .paidCount(acc.getPaidCount())
                        .failedCount(acc.getFailedCount())
                        .cancelledCount(acc.getCancelledCount())
                        .build())
                .collect(Collectors.toList());
    }

    public Page<PayOSTransactionDto> getPayOsTransactions(LocalDate from,
                                                          LocalDate to,
                                                          Pageable pageable) {
        DateRange range = resolveRange(from, to,
                bookingRepository.findEarliestCreatedAt(),
                bookingRepository.findLatestCreatedAt());
        Page<Booking> page = bookingRepository
                .findByCreatedAtBetween(range.start(), range.end(), pageable);

        List<PayOSTransactionDto> dtos = page.getContent().stream()
                .map(this::mapBookingToTransaction)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private PayOSTransactionDto mapBookingToTransaction(Booking booking) {
        PaymentStatus paymentStatus = booking.getPaymentStatus();
        return PayOSTransactionDto.builder()
                .bookingCode(booking.getBookingCode())
                .provider(normalizePaymentMethod(booking.getPaymentMethod()))
                .amount(safeAmount(booking))
                .status(paymentStatus != null ? paymentStatus.name() : PaymentStatus.Unpaid.name())
                .createdAt(booking.getCreatedAt())
                .rawPayloadShort(buildBookingSnapshot(booking))
                .build();
    }

    private String buildBookingSnapshot(Booking booking) {
        return String.format(Locale.ROOT,
                "Booking #%s | Status=%s | PaymentStatus=%s | FinalAmount=%s",
                booking.getBookingCode(),
                booking.getBookingStatus() != null ? booking.getBookingStatus().name() : BookingStatus.Pending.name(),
                booking.getPaymentStatus() != null ? booking.getPaymentStatus().name() : PaymentStatus.Unpaid.name(),
                safeAmount(booking));
    }

    private SummaryMetrics computeSummaryMetrics(List<Booking> bookings) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        long paidOrders = 0;
        long failedOrders = 0;
        long cancelledOrders = 0;
        Map<String, BigDecimal> revenueByPaymentMethod = new LinkedHashMap<>();

        for (Booking booking : bookings) {
            PaymentStatus paymentStatus = booking.getPaymentStatus();
            BookingStatus bookingStatus = booking.getBookingStatus();
            BigDecimal amount = safeAmount(booking);

            if (paymentStatus == PaymentStatus.Paid) {
                paidOrders++;
                totalRevenue = totalRevenue.add(amount);
                String method = normalizePaymentMethod(booking.getPaymentMethod());
                revenueByPaymentMethod.merge(method, amount, BigDecimal::add);
            } else if (paymentStatus == PaymentStatus.Failed) {
                failedOrders++;
            }
            if (bookingStatus == BookingStatus.Cancelled) {
                cancelledOrders++;
            }
        }

        return new SummaryMetrics(totalRevenue,
                bookings.size(),
                paidOrders,
                failedOrders,
                cancelledOrders,
                revenueByPaymentMethod);
    }

    private BigDecimal calculateGrowthPercent(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(ONE_HUNDRED)
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> sortRevenueMap(Map<String, BigDecimal> source) {
        return source.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private BigDecimal safeAmount(Booking booking) {
        return booking.getFinalAmount() != null ? booking.getFinalAmount() : BigDecimal.ZERO;
    }

    private DateRange resolveRange(LocalDate from,
                                   LocalDate to,
                                   OffsetDateTime earliest,
                                   OffsetDateTime latest) {
        LocalDate defaultStart = from;
        LocalDate defaultEnd = to;
        LocalDate now = LocalDate.now(TimeProvider.VN_ZONE_ID);

        if (defaultStart == null) {
            defaultStart = earliest != null ? toLocalDate(earliest) : now.minusDays(30);
        }
        if (defaultEnd == null) {
            defaultEnd = latest != null ? toLocalDate(latest) : now;
        }
        if (defaultEnd.isBefore(defaultStart)) {
            LocalDate temp = defaultStart;
            defaultStart = defaultEnd;
            defaultEnd = temp;
        }

        return new DateRange(startOfDay(defaultStart), endOfDay(defaultEnd), defaultStart, defaultEnd);
    }

    private OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay(TimeProvider.VN_ZONE_ID).toOffsetDateTime();
    }

    private OffsetDateTime endOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX).atZone(TimeProvider.VN_ZONE_ID).toOffsetDateTime();
    }

    private LocalDate toLocalDate(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(TimeProvider.VN_ZONE_ID).toLocalDate();
    }

    private String normalizePaymentMethod(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return PAYMENT_UNKNOWN;
        }
        String raw = rawValue.trim();
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.contains("vietqr") || normalized.contains("payos") || normalized.contains("qr")) {
            return PAYMENT_PAYOS;
        }
        if (normalized.contains("cash")) {
            return PAYMENT_CASH;
        }
        if (normalized.contains("bank") || normalized.contains("transfer")) {
            return PAYMENT_BANK;
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private String truncatePayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        String trimmed = payload.trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }

    private record DateRange(OffsetDateTime start,
                             OffsetDateTime end,
                             LocalDate startDate,
                             LocalDate endDate) {

        private DateRange previous() {
            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            days = Math.max(days, 1);
            LocalDate previousEnd = startDate.minusDays(1);
            LocalDate previousStart = previousEnd.minusDays(days - 1);
            return new DateRange(
                    previousStart.atStartOfDay(TimeProvider.VN_ZONE_ID).toOffsetDateTime(),
                    previousEnd.atTime(LocalTime.MAX).atZone(TimeProvider.VN_ZONE_ID).toOffsetDateTime(),
                    previousStart,
                    previousEnd);
        }
    }

    private static class SummaryMetrics {
        private final BigDecimal totalRevenue;
        private final long totalOrders;
        private final long totalPaidOrders;
        private final long totalFailedOrders;
        private final long totalCancelledOrders;
        private final Map<String, BigDecimal> revenueByPaymentMethod;

        SummaryMetrics(BigDecimal totalRevenue,
                       long totalOrders,
                       long totalPaidOrders,
                       long totalFailedOrders,
                       long totalCancelledOrders,
                       Map<String, BigDecimal> revenueByPaymentMethod) {
            this.totalRevenue = totalRevenue;
            this.totalOrders = totalOrders;
            this.totalPaidOrders = totalPaidOrders;
            this.totalFailedOrders = totalFailedOrders;
            this.totalCancelledOrders = totalCancelledOrders;
            this.revenueByPaymentMethod = revenueByPaymentMethod;
        }

        public BigDecimal totalRevenue() {
            return totalRevenue;
        }

        public long totalOrders() {
            return totalOrders;
        }

        public long totalPaidOrders() {
            return totalPaidOrders;
        }

        public long totalFailedOrders() {
            return totalFailedOrders;
        }

        public long totalCancelledOrders() {
            return totalCancelledOrders;
        }

        public Map<String, BigDecimal> revenueByPaymentMethod() {
            return revenueByPaymentMethod;
        }
    }

    private static class PaymentMethodAccumulator {
        private long count;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        void increment(BigDecimal amount) {
            this.count++;
            this.totalAmount = this.totalAmount.add(amount);
        }

        long getCount() {
            return count;
        }

        BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }

    private static class OrderStatusAccumulator {
        private final String bookingStatus;
        private final String paymentStatus;
        private long count;
        private BigDecimal revenue = BigDecimal.ZERO;

        OrderStatusAccumulator(String bookingStatus, String paymentStatus) {
            this.bookingStatus = bookingStatus;
            this.paymentStatus = paymentStatus;
        }

        void increment(BigDecimal amount) {
            this.count++;
            this.revenue = this.revenue.add(amount);
        }

        String getBookingStatus() {
            return bookingStatus;
        }

        String getPaymentStatus() {
            return paymentStatus;
        }

        long getCount() {
            return count;
        }

        BigDecimal getRevenue() {
            return revenue;
        }
    }

    private static class TrendAccumulator {
        private final LocalDate date;
        private BigDecimal paidRevenue = BigDecimal.ZERO;
        private long paidCount;
        private long failedCount;
        private long cancelledCount;

        TrendAccumulator(LocalDate date) {
            this.date = date;
        }

        void addPaid(BigDecimal amount) {
            this.paidRevenue = this.paidRevenue.add(amount);
            this.paidCount++;
        }

        void incrementFailed() {
            this.failedCount++;
        }

        void incrementCancelled() {
            this.cancelledCount++;
        }

        LocalDate getDate() {
            return date;
        }

        BigDecimal getPaidRevenue() {
            return paidRevenue;
        }

        long getPaidCount() {
            return paidCount;
        }

        long getFailedCount() {
            return failedCount;
        }

        long getCancelledCount() {
            return cancelledCount;
        }
    }
}
