package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.dto.admin.analytics.AnalyticsSummaryRangeDto;
import com.cinema.hub.backend.dto.admin.analytics.OrderStatusBreakdownDto;
import com.cinema.hub.backend.dto.admin.analytics.PayOSTransactionDto;
import com.cinema.hub.backend.dto.admin.analytics.PaymentMethodBreakdownDto;
import com.cinema.hub.backend.dto.admin.analytics.RevenueTrendPointDto;
import com.cinema.hub.backend.service.AdminAnalyticsService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping("/summary-range")
    public ResponseEntity<AnalyticsSummaryRangeDto> getSummaryRange(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminAnalyticsService.getSummaryRange(from, to));
    }

    @GetMapping("/payment-method-range")
    public ResponseEntity<List<PaymentMethodBreakdownDto>> getPaymentMethodBreakdown(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminAnalyticsService.getPaymentMethodBreakdown(from, to));
    }

    @GetMapping("/status-range")
    public ResponseEntity<List<OrderStatusBreakdownDto>> getStatusBreakdown(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminAnalyticsService.getStatusBreakdown(from, to));
    }

    @GetMapping("/revenue-range")
    public ResponseEntity<List<RevenueTrendPointDto>> getRevenueTrend(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(adminAnalyticsService.getRevenueTrend(from, to));
    }

    @GetMapping("/transactions-range")
    public ResponseEntity<Page<PayOSTransactionDto>> getTransactions(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PayOSTransactionDto> data = adminAnalyticsService.getPayOsTransactions(from, to, pageable);
        return ResponseEntity.ok(data);
    }
}
