package com.cinema.hub.backend.repository;

import com.cinema.hub.backend.dto.admin.dashboard.OrderStatusDTO;
import com.cinema.hub.backend.dto.admin.dashboard.OrderTableDTO;
import com.cinema.hub.backend.dto.admin.dashboard.PageDTO;
import com.cinema.hub.backend.dto.admin.dashboard.PaymentChannelDTO;
import com.cinema.hub.backend.dto.admin.dashboard.RevenueByDayDTO;
import com.cinema.hub.backend.util.TimeProvider;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.math.RoundingMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class DashboardRepository {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_UNPAID = "UNPAID";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_FAILED = "FAILED";

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal getRevenueBetween(OffsetDateTime start, OffsetDateTime end) {
        String sql = """
            SELECT COALESCE(SUM(b.FinalAmount), 0)
            FROM Bookings b
            WHERE b.PaymentStatus = 'Paid'
              AND COALESCE(b.PaidAt, b.CreatedAt) >= ?
              AND COALESCE(b.PaidAt, b.CreatedAt) < ?
        """;
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, toTimestamp(start), toTimestamp(end));
    }

    public long countOrdersBetween(OffsetDateTime start, OffsetDateTime end) {
        String sql = """
            SELECT COUNT(*)
            FROM Bookings b
            WHERE b.CreatedAt >= ?
              AND b.CreatedAt < ?
        """;
        Long value = jdbcTemplate.queryForObject(sql, Long.class, toTimestamp(start), toTimestamp(end));
        return value != null ? value : 0L;
    }

    public long countOrdersByStatuses(OffsetDateTime start, OffsetDateTime end, List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        List<String> normalized = statuses.stream()
                .filter(StringUtils::hasText)
                .map(status -> status.trim().toUpperCase(Locale.ROOT))
                .toList();
        if (normalized.isEmpty()) {
            return 0L;
        }
        String placeholders = String.join(",", Collections.nCopies(normalized.size(), "?"));
        String sql = """
            SELECT COUNT(*)
            FROM Bookings b
            WHERE b.CreatedAt >= ?
              AND b.CreatedAt < ?
              AND UPPER(b.PaymentStatus) IN (%s)
        """.formatted(placeholders);
        List<Object> params = new ArrayList<>();
        params.add(toTimestamp(start));
        params.add(toTimestamp(end));
        params.addAll(normalized);
        Long value = jdbcTemplate.queryForObject(sql, params.toArray(), Long.class);
        return value != null ? value : 0L;
    }

    public long countSeatsSold(OffsetDateTime start, OffsetDateTime end) {
        String sql = """
            SELECT COALESCE(SUM(seatInfo.SeatCount), 0)
            FROM Bookings b
            OUTER APPLY (
                SELECT COUNT(*) AS SeatCount
                FROM BookingSeats bs
                WHERE bs.BookingId = b.BookingId
            ) seatInfo
            WHERE b.CreatedAt >= ?
              AND b.CreatedAt < ?
              AND UPPER(b.PaymentStatus) = 'PAID'
        """;
        Long value = jdbcTemplate.queryForObject(sql, Long.class, toTimestamp(start), toTimestamp(end));
        return value != null ? value : 0L;
    }

    public List<RevenueByDayDTO> getRevenueByDay(OffsetDateTime start, OffsetDateTime end) {
        String sql = """
            SELECT CAST(COALESCE(b.PaidAt, b.CreatedAt) AS DATE) AS RevenueDate,
                   COALESCE(SUM(b.FinalAmount), 0) AS TotalRevenue,
                   SUM(CASE WHEN UPPER(b.PaymentStatus) = 'PAID' THEN 1 ELSE 0 END) AS PaidCount
            FROM Bookings b
            WHERE COALESCE(b.PaidAt, b.CreatedAt) >= ?
              AND COALESCE(b.PaidAt, b.CreatedAt) < ?
            GROUP BY CAST(COALESCE(b.PaidAt, b.CreatedAt) AS DATE)
            ORDER BY RevenueDate ASC
        """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> RevenueByDayDTO.builder()
                .date(rs.getObject("RevenueDate", LocalDate.class))
                .revenue(rs.getBigDecimal("TotalRevenue"))
                .successfulOrders(rs.getLong("PaidCount"))
                .build(), toTimestamp(start), toTimestamp(end));
    }

    public List<PaymentChannelDTO> getPaymentChannels(OffsetDateTime start, OffsetDateTime end) {
        String sql = """
            SELECT COALESCE(NULLIF(pl.Provider, ''), NULLIF(b.PaymentMethod, ''), 'UNKNOWN') AS Channel,
                   COUNT(*) AS TotalOrders,
                   COALESCE(SUM(b.FinalAmount), 0) AS TotalAmount
            FROM Bookings b
            OUTER APPLY (
                SELECT TOP 1 p.Provider
                FROM PaymentLogs p
                WHERE p.BookingId = b.BookingId
                ORDER BY p.CreatedAt DESC
            ) pl
            WHERE COALESCE(b.PaidAt, b.CreatedAt) >= ?
              AND COALESCE(b.PaidAt, b.CreatedAt) < ?
              AND UPPER(b.PaymentStatus) = 'PAID'
            GROUP BY COALESCE(NULLIF(pl.Provider, ''), NULLIF(b.PaymentMethod, ''), 'UNKNOWN')
        """;
        List<PaymentChannelDTO> rows = jdbcTemplate.query(sql, (rs, rowNum) -> PaymentChannelDTO.builder()
                .channel(rs.getString("Channel"))
                .totalOrders(rs.getLong("TotalOrders"))
                .totalAmount(rs.getBigDecimal("TotalAmount"))
                .build(), toTimestamp(start), toTimestamp(end));
        if (rows.isEmpty()) {
            return rows;
        }
        BigDecimal total = rows.stream()
                .map(PaymentChannelDTO::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal denominator = total.compareTo(BigDecimal.ZERO) > 0 ? total : BigDecimal.ONE;
        rows.forEach(row -> {
            BigDecimal amount = row.getTotalAmount() != null ? row.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal ratio = amount.multiply(BigDecimal.valueOf(100))
                    .divide(denominator, 2, RoundingMode.HALF_UP);
            row.setRatio(ratio);
        });
        rows.sort(Comparator.comparing(PaymentChannelDTO::getTotalAmount, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
        return rows;
    }

    public List<OrderStatusDTO> getOrderStatusBreakdown(OffsetDateTime start, OffsetDateTime end) {
        String sql = """
            SELECT CASE
                       WHEN UPPER(b.PaymentStatus) = 'PAID' THEN 'Paid'
                       WHEN UPPER(b.PaymentStatus) = 'UNPAID' THEN 'Unpaid'
                       WHEN UPPER(b.PaymentStatus) = 'FAILED' THEN 'Failed'
                       ELSE 'Others'
                   END AS StatusLabel,
                   COUNT(*) AS TotalOrders,
                   COALESCE(SUM(b.FinalAmount), 0) AS TotalAmount
            FROM Bookings b
            WHERE b.CreatedAt >= ?
              AND b.CreatedAt < ?
            GROUP BY CASE
                       WHEN UPPER(b.PaymentStatus) = 'PAID' THEN 'Paid'
                       WHEN UPPER(b.PaymentStatus) = 'UNPAID' THEN 'Unpaid'
                       WHEN UPPER(b.PaymentStatus) = 'FAILED' THEN 'Failed'
                       ELSE 'Others'
                   END
        """;
        List<OrderStatusDTO> rows = jdbcTemplate.query(sql, (rs, rowNum) -> OrderStatusDTO.builder()
                .status(rs.getString("StatusLabel"))
                .count(rs.getLong("TotalOrders"))
                .totalAmount(rs.getBigDecimal("TotalAmount"))
                .build(), toTimestamp(start), toTimestamp(end));
        long totalOrders = rows.stream().mapToLong(OrderStatusDTO::getCount).sum();
        if (totalOrders == 0) {
            return rows;
        }
        rows.forEach(row -> {
            BigDecimal ratio = BigDecimal.valueOf(row.getCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
            row.setRatio(ratio);
        });
        rows.sort(Comparator.comparing(OrderStatusDTO::getCount).reversed());
        return rows;
    }

    public PageDTO<OrderTableDTO> getOrderTable(OffsetDateTime createdStart,
                                                OffsetDateTime createdEnd,
                                                OffsetDateTime paidStart,
                                                OffsetDateTime paidEnd,
                                                String paymentMethod,
                                                String paymentStatus,
                                                String query,
                                                int page,
                                                int size) {
        QueryParts queryParts = buildOrderTableFilters(createdStart, createdEnd, paidStart, paidEnd,
                paymentMethod, paymentStatus, query);
        String baseSelect = orderTableBaseSelect() + queryParts.whereClause();
        String dataSql = baseSelect + " ORDER BY COALESCE(b.PaidAt, b.CreatedAt) DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> dataParams = new ArrayList<>(queryParts.params());
        dataParams.add(page * size);
        dataParams.add(size);
        List<OrderTableDTO> rows = jdbcTemplate.query(dataSql, dataParams.toArray(), orderTableMapper());
        String countSql = "SELECT COUNT(*) FROM (" + baseSelect + ") AS CountQuery";
        Long total = jdbcTemplate.queryForObject(countSql, queryParts.params().toArray(), Long.class);
        long totalElements = total != null ? total : 0L;
        return PageDTO.of(rows, page, size, totalElements);
    }

    public List<OrderTableDTO> getOrderTableForExport(OffsetDateTime createdStart,
                                                      OffsetDateTime createdEnd,
                                                      OffsetDateTime paidStart,
                                                      OffsetDateTime paidEnd,
                                                      String paymentMethod,
                                                      String paymentStatus,
                                                      String query) {
        QueryParts queryParts = buildOrderTableFilters(createdStart, createdEnd, paidStart, paidEnd,
                paymentMethod, paymentStatus, query);
        String sql = orderTableBaseSelect() + queryParts.whereClause()
                + " ORDER BY COALESCE(b.PaidAt, b.CreatedAt) DESC";
        return jdbcTemplate.query(sql, queryParts.params().toArray(), orderTableMapper());
    }

    private String orderTableBaseSelect() {
        return """
            SELECT b.BookingId,
                   b.UserId,
                   b.CreatedByStaffId,
                   b.PaymentMethod,
                   b.TotalAmount,
                   b.FinalAmount,
                   b.CreatedAt,
                   b.PaidAt,
                   b.BookingCode,
                   b.PaymentStatus,
                   b.ShowtimeId,
                   b.CustomerEmail,
                   b.CustomerPhone,
                   pl.Provider AS Provider,
                   pl.ProviderTransactionId AS ProviderTransactionId,
                   pl.RawMessage AS RawMessage,
                   seatInfo.SeatCount AS SeatCount
            FROM Bookings b
            OUTER APPLY (
                SELECT TOP 1 p.Provider, p.ProviderTransactionId, p.RawMessage
                FROM PaymentLogs p
                WHERE p.BookingId = b.BookingId
                ORDER BY p.CreatedAt DESC
            ) pl
            OUTER APPLY (
                SELECT COUNT(*) AS SeatCount
                FROM BookingSeats bs
                WHERE bs.BookingId = b.BookingId
            ) seatInfo
        """;
    }

    private QueryParts buildOrderTableFilters(OffsetDateTime createdStart,
                                              OffsetDateTime createdEnd,
                                              OffsetDateTime paidStart,
                                              OffsetDateTime paidEnd,
                                              String paymentMethod,
                                              String paymentStatus,
                                              String query) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (createdStart != null) {
            where.append(" AND b.CreatedAt >= ?");
            params.add(toTimestamp(createdStart));
        }
        if (createdEnd != null) {
            where.append(" AND b.CreatedAt < ?");
            params.add(toTimestamp(createdEnd));
        }
        if (paidStart != null) {
            where.append(" AND b.PaidAt IS NOT NULL AND b.PaidAt >= ?");
            params.add(toTimestamp(paidStart));
        }
        if (paidEnd != null) {
            where.append(" AND b.PaidAt IS NOT NULL AND b.PaidAt < ?");
            params.add(toTimestamp(paidEnd));
        }
        if (StringUtils.hasText(paymentMethod)) {
            where.append(" AND LOWER(COALESCE(NULLIF(pl.Provider, ''), NULLIF(b.PaymentMethod, ''), '')) = ?");
            params.add(paymentMethod.trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(paymentStatus)) {
            where.append(" AND LOWER(b.PaymentStatus) = ?");
            params.add(paymentStatus.trim().toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(query)) {
            String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            where.append("""
                    AND (
                        LOWER(b.BookingCode) LIKE ?
                        OR LOWER(COALESCE(NULLIF(pl.ProviderTransactionId, ''), '')) LIKE ?
                        OR LOWER(COALESCE(NULLIF(pl.Provider, ''), '')) LIKE ?
                        OR LOWER(COALESCE(b.PaymentStatus, '')) LIKE ?
                        OR LOWER(COALESCE(b.CustomerEmail, '')) LIKE ?
                        OR LOWER(COALESCE(b.CustomerPhone, '')) LIKE ?
                        OR LOWER(COALESCE(b.PaymentMethod, '')) LIKE ?
                        OR CAST(b.UserId AS NVARCHAR(50)) LIKE ?
                        OR CAST(b.CreatedByStaffId AS NVARCHAR(50)) LIKE ?
                    )
                    """);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        return new QueryParts(where.toString(), params);
    }

    private RowMapper<OrderTableDTO> orderTableMapper() {
        return (ResultSet rs, int rowNum) -> {
            OffsetDateTime createdAt = getOffsetDateTime(rs, "CreatedAt");
            OffsetDateTime paidAt = getOffsetDateTime(rs, "PaidAt");
            String bookingCode = rs.getString("BookingCode");
            String provider = rs.getString("Provider");
            String method = rs.getString("PaymentMethod");
            String paymentChannel = StringUtils.hasText(provider) ? provider : method;
            String rawMessage = rs.getString("RawMessage");
            int seats = rs.getInt("SeatCount");
            if (rs.wasNull()) {
                seats = 0;
            }
            String description = StringUtils.hasText(rawMessage)
                    ? rawMessage
                    : "Thanh toan don " + bookingCode;
            String detail = "Showtime #" + rs.getInt("ShowtimeId");
            if (seats > 0) {
                detail += " - " + seats + " ve";
            }
            return OrderTableDTO.builder()
                    .bookingId(rs.getInt("BookingId"))
                    .paymentChannel(paymentChannel)
                    .paymentMethod(method)
                    .paymentProvider(provider)
                    .totalAmount(rs.getBigDecimal("TotalAmount"))
                    .finalAmount(rs.getBigDecimal("FinalAmount"))
                    .createdAt(createdAt)
                    .paidAt(paidAt)
                    .description(description)
                    .accountNumber(rs.getString("ProviderTransactionId"))
                    .customerEmail(rs.getString("CustomerEmail"))
                    .customerPhone(rs.getString("CustomerPhone"))
                    .userId((Integer) rs.getObject("UserId"))
                    .createdByStaffId((Integer) rs.getObject("CreatedByStaffId"))
                    .bookingCode(bookingCode)
                    .paymentStatus(rs.getString("PaymentStatus"))
                    .detail(detail)
                    .showtimeId(rs.getInt("ShowtimeId"))
                    .build();
        };
    }

    private Timestamp toTimestamp(OffsetDateTime time) {
        return time != null ? Timestamp.from(time.toInstant()) : null;
    }

    private OffsetDateTime getOffsetDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atZone(TimeProvider.VN_ZONE_ID).toOffsetDateTime();
    }

    private record QueryParts(String whereClause, List<Object> params) {
    }
}
