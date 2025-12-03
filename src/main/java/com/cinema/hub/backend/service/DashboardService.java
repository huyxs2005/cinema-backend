package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.admin.dashboard.DashboardSummaryDTO;
import com.cinema.hub.backend.dto.admin.dashboard.OrderStatusDTO;
import com.cinema.hub.backend.dto.admin.dashboard.OrderTableDTO;
import com.cinema.hub.backend.dto.admin.dashboard.PageDTO;
import com.cinema.hub.backend.dto.admin.dashboard.PaymentChannelDTO;
import com.cinema.hub.backend.dto.admin.dashboard.RevenueByDayDTO;
import com.cinema.hub.backend.repository.DashboardRepository;
import com.cinema.hub.backend.util.TimeProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DashboardService {

    private static final ZoneId ZONE_ID = TimeProvider.VN_ZONE_ID;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardSummaryDTO getSummary(LocalDate startDate, LocalDate endDate) {
        OffsetDateTime now = TimeProvider.now();
        DateRange selectedRange = resolveRange(startDate, endDate, now);
        DateRange todayRange = dateRangeForToday(now);
        DateRange yesterdayRange = dateRangeForYesterday(now);
        DateRange weekRange = dateRangeForThisWeek(now);
        DateRange monthRange = dateRangeForThisMonth(now);
        DateRange yearRange = dateRangeForThisYear(now);
        DateRange lastMonthRange = dateRangeForLastMonth(now);
        DateRange lastYearRange = dateRangeForLastYear(now);

        BigDecimal revenueToday = dashboardRepository.getRevenueBetween(todayRange.start(), todayRange.end());
        BigDecimal revenueYesterday = dashboardRepository.getRevenueBetween(yesterdayRange.start(), yesterdayRange.end());
        BigDecimal revenueThisWeek = dashboardRepository.getRevenueBetween(weekRange.start(), weekRange.end());
        BigDecimal revenueThisMonth = dashboardRepository.getRevenueBetween(monthRange.start(), monthRange.end());
        BigDecimal revenueThisYear = dashboardRepository.getRevenueBetween(yearRange.start(), yearRange.end());
        BigDecimal revenueLastMonth = dashboardRepository.getRevenueBetween(lastMonthRange.start(), lastMonthRange.end());
        BigDecimal revenueLastYear = dashboardRepository.getRevenueBetween(lastYearRange.start(), lastYearRange.end());
        BigDecimal revenueSelected = dashboardRepository.getRevenueBetween(selectedRange.start(), selectedRange.end());

        long totalOrders = dashboardRepository.countOrdersBetween(selectedRange.start(), selectedRange.end());
        long completedOrders = dashboardRepository.countOrdersByStatuses(selectedRange.start(), selectedRange.end(),
                List.of("PAID"));
        long pendingOrders = dashboardRepository.countOrdersByStatuses(selectedRange.start(), selectedRange.end(),
                List.of("UNPAID"));
        long failedOrders = dashboardRepository.countOrdersByStatuses(selectedRange.start(), selectedRange.end(),
                List.of("FAILED"));
        long seatsSold = dashboardRepository.countSeatsSold(selectedRange.start(), selectedRange.end());

        BigDecimal paymentRate = totalOrders > 0
                ? BigDecimal.valueOf(completedOrders)
                        .multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return DashboardSummaryDTO.builder()
                .revenueToday(revenueToday)
                .revenueYesterday(revenueYesterday)
                .revenueThisWeek(revenueThisWeek)
                .revenueThisMonth(revenueThisMonth)
                .revenueThisYear(revenueThisYear)
                .revenueLastMonth(revenueLastMonth)
                .revenueLastYear(revenueLastYear)
                .revenueCustom(revenueSelected)
                .customStart(selectedRange.start())
                .customEnd(selectedRange.end())
                .selectedRevenue(revenueSelected)
                .selectedRangeStart(selectedRange.start())
                .selectedRangeEnd(selectedRange.end())
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .failedOrders(failedOrders)
                .seatsSold(seatsSold)
                .paymentSuccessRate(paymentRate)
                .generatedAt(now)
                .build();
    }

    public List<RevenueByDayDTO> getRevenueRange(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate, TimeProvider.now());
        List<RevenueByDayDTO> raw = dashboardRepository.getRevenueByDay(range.start(), range.end());
        Map<LocalDate, RevenueByDayDTO> byDate = raw.stream()
                .filter(dto -> dto.getDate() != null)
                .collect(Collectors.toMap(RevenueByDayDTO::getDate, dto -> dto, (a, b) -> a, LinkedHashMap::new));
        LocalDate cursor = range.start().toLocalDate();
        LocalDate endInclusive = range.end().minusDays(1).toLocalDate();
        List<RevenueByDayDTO> normalized = new ArrayList<>();
        while (!cursor.isAfter(endInclusive)) {
            RevenueByDayDTO dto = byDate.get(cursor);
            if (dto == null) {
                dto = RevenueByDayDTO.builder()
                        .date(cursor)
                        .revenue(BigDecimal.ZERO)
                        .successfulOrders(0L)
                        .build();
            }
            normalized.add(dto);
            cursor = cursor.plusDays(1);
        }
        return normalized;
    }

    public List<PaymentChannelDTO> getPaymentChannels(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate, TimeProvider.now());
        return dashboardRepository.getPaymentChannels(range.start(), range.end());
    }

    public List<OrderStatusDTO> getOrderStatuses(LocalDate startDate, LocalDate endDate) {
        DateRange range = resolveRange(startDate, endDate, TimeProvider.now());
        return dashboardRepository.getOrderStatusBreakdown(range.start(), range.end());
    }

    public PageDTO<OrderTableDTO> getOrderTable(LocalDate createdStart,
                                                LocalDate createdEnd,
                                                LocalDate paidStart,
                                                LocalDate paidEnd,
                                                String paymentMethod,
                                                String paymentStatus,
                                                String search,
                                                int page,
                                                int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        OffsetDateTime createdFrom = toStartOfDay(createdStart);
        OffsetDateTime createdTo = toEndOfDay(createdEnd);
        OffsetDateTime paidFrom = toStartOfDay(paidStart);
        OffsetDateTime paidTo = toEndOfDay(paidEnd);
        return dashboardRepository.getOrderTable(createdFrom, createdTo, paidFrom, paidTo,
                sanitize(paymentMethod), sanitize(paymentStatus), sanitize(search), safePage, safeSize);
    }

    public byte[] exportOrders(LocalDate createdStart,
                               LocalDate createdEnd,
                               LocalDate paidStart,
                               LocalDate paidEnd,
                               String paymentMethod,
                               String paymentStatus,
                               String search) {
        OffsetDateTime createdFrom = toStartOfDay(createdStart);
        OffsetDateTime createdTo = toEndOfDay(createdEnd);
        OffsetDateTime paidFrom = toStartOfDay(paidStart);
        OffsetDateTime paidTo = toEndOfDay(paidEnd);
        List<OrderTableDTO> rows = dashboardRepository.getOrderTableForExport(createdFrom, createdTo, paidFrom, paidTo,
                sanitize(paymentMethod), sanitize(paymentStatus), sanitize(search));
        return buildExcel(rows);
    }

    private byte[] buildExcel(List<OrderTableDTO> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Dashboard");
            createHeader(sheet);
            int rowIndex = 1;
            for (OrderTableDTO dto : rows) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, dto.getBookingCode());
                writeCell(row, 1, dto.getAccountNumber());
                writeCell(row, 2, dto.getCreatedAt() != null ? dto.getCreatedAt().toString() : "");
                writeCell(row, 3, dto.getDetail());
                String createdBy = dto.getCreatedByStaffId() != null
                        ? "Staff #" + dto.getCreatedByStaffId()
                        : dto.getUserId() != null ? "User #" + dto.getUserId() : "";
                writeCell(row, 4, createdBy);
                writeCell(row, 5, dto.getCustomerEmail());
                writeCell(row, 6, dto.getCustomerPhone());
                writeCell(row, 7, dto.getFinalAmount() != null ? dto.getFinalAmount().toPlainString() : "0");
                writeCell(row, 8, dto.getPaymentStatus());
            }
            for (int i = 0; i <= 8; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot build dashboard Excel export", ex);
        }
    }

    private void createHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] titles = {
                "Ma don hang",
                "Ma thanh toan",
                "Ngay tao",
                "Chi tiet",
                "Tao boi",
                "Email nguoi mua",
                "So dien thoai",
                "Tien thanh toan",
                "Trang thai"
        };
        for (int i = 0; i < titles.length; i++) {
            writeCell(header, i, titles[i]);
        }
    }

    private void writeCell(Row row, int index, String value) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value != null ? value : "");
    }

    private OffsetDateTime toStartOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay(ZONE_ID).toOffsetDateTime();
    }

    private OffsetDateTime toEndOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.plusDays(1).atStartOfDay(ZONE_ID).toOffsetDateTime();
    }

    private String sanitize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }

    private DateRange resolveRange(LocalDate startDate, LocalDate endDate, OffsetDateTime now) {
        LocalDate fallback = now.toLocalDate();
        LocalDate safeStart = Objects.requireNonNullElse(startDate, Objects.requireNonNullElse(endDate, fallback));
        LocalDate safeEnd = Objects.requireNonNullElse(endDate, safeStart);
        if (safeEnd.isBefore(safeStart)) {
            LocalDate temp = safeStart;
            safeStart = safeEnd;
            safeEnd = temp;
        }
        return new DateRange(
                safeStart.atStartOfDay(ZONE_ID).toOffsetDateTime(),
                safeEnd.plusDays(1).atStartOfDay(ZONE_ID).toOffsetDateTime());
    }

    private DateRange dateRangeForToday(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        return new DateRange(
                today.atStartOfDay(ZONE_ID).toOffsetDateTime(),
                today.plusDays(1).atStartOfDay(ZONE_ID).toOffsetDateTime());
    }

    private DateRange dateRangeForYesterday(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate yesterday = today.minusDays(1);
        return new DateRange(
                yesterday.atStartOfDay(ZONE_ID).toOffsetDateTime(),
                today.atStartOfDay(ZONE_ID).toOffsetDateTime());
    }

    private DateRange dateRangeForThisWeek(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(7);
        return new DateRange(
                start.atStartOfDay(ZONE_ID).toOffsetDateTime(),
                end.atStartOfDay(ZONE_ID).toOffsetDateTime());
    }

    private DateRange dateRangeForThisMonth(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate start = today.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        return new DateRange(
                start.atStartOfDay(ZONE_ID).toOffsetDateTime(),
                end.atStartOfDay(ZONE_ID).toOffsetDateTime());
    }

    private DateRange dateRangeForThisYear(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate start = today.withDayOfYear(1);
        LocalDate end = start.plusYears(1);
        return new DateRange(
                start.atStartOfDay(ZONE_ID).toOffsetDateTime(),
                end.atStartOfDay(ZONE_ID).toOffsetDateTime());
    }

    private DateRange dateRangeForLastMonth(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate start = today.minusMonths(1).withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        return new DateRange(
                start.atStartOfDay(ZONE_ID).toOffsetDateTime(),
                end.atStartOfDay(ZONE_ID).toOffsetDateTime());
    }

    private DateRange dateRangeForLastYear(OffsetDateTime now) {
        LocalDate today = now.toLocalDate();
        LocalDate start = today.minusYears(1).withDayOfYear(1);
        LocalDate end = start.plusYears(1);
        return new DateRange(
                start.atStartOfDay(ZONE_ID).toOffsetDateTime(),
                end.atStartOfDay(ZONE_ID).toOffsetDateTime());
    }

    private record DateRange(OffsetDateTime start, OffsetDateTime end) {
    }
}


