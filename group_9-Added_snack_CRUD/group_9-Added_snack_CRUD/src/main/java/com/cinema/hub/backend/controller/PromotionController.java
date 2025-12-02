package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.promotion.PromotionDetailDto;
import com.cinema.hub.backend.dto.promotion.PromotionSummaryDto;
import com.cinema.hub.backend.service.PromotionService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/khuyen-mai")
    public String listPromotions(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "9") int size,
                                 Model model) {
        int normalizedSize = Math.min(Math.max(size, 1), 24);
        Pageable pageable = PageRequest.of(page, normalizedSize,
                Sort.by(Sort.Order.desc("publishedDate"), Sort.Order.desc("createdAt")));
        PageResponse<PromotionSummaryDto> promoPage = promotionService.getPublicPromotions(pageable);
        model.addAttribute("promotionsPage", promoPage);
        model.addAttribute("currentPage", promoPage.getPage());
        model.addAttribute("pageSize", promoPage.getSize());
        return "promotions";
    }

    @GetMapping("/khuyen-mai/{slug}")
    public String promotionDetail(@PathVariable String slug, Model model) {
        try {
            PromotionDetailDto detail = promotionService.getDetailBySlug(slug);
            model.addAttribute("promotion", detail);
            return "promotion-detail";
        } catch (ResponseStatusException ex) {
            if (Objects.equals(ex.getStatusCode(), HttpStatus.NOT_FOUND)) {
                return "redirect:/khuyen-mai";
            }
            throw ex;
        }
    }
}
