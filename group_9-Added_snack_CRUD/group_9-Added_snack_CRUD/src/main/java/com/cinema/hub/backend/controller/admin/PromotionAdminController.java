package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.promotion.PromotionRequestDto;
import com.cinema.hub.backend.dto.promotion.PromotionResponseDto;
import com.cinema.hub.backend.dto.promotion.PromotionSummaryDto;
import com.cinema.hub.backend.service.PromotionService;
import com.cinema.hub.backend.util.PaginationUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/promotions")
@RequiredArgsConstructor
public class PromotionAdminController {

    private final PromotionService promotionService;

    @GetMapping
    public PageResponse<PromotionResponseDto> search(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Boolean active,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                     LocalDate fromDate,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                     LocalDate toDate,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size,
                                                     @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtil.create(page, size, sort);
        return promotionService.search(keyword, active, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    public PromotionResponseDto getById(@PathVariable Long id) {
        return promotionService.getById(id);
    }

    @PostMapping
    public PromotionResponseDto create(@Valid @RequestBody PromotionRequestDto requestDto) {
        return promotionService.create(requestDto);
    }

    @PutMapping("/{id}")
    public PromotionResponseDto update(@PathVariable Long id,
                                       @Valid @RequestBody PromotionRequestDto requestDto) {
        return promotionService.update(id, requestDto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        promotionService.delete(id);
    }

    @GetMapping("/options")
    public List<PromotionSummaryDto> options() {
        return promotionService.getActiveOptions();
    }
}
