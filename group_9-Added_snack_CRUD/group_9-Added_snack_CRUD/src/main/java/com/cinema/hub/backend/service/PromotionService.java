package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.promotion.PromotionDetailDto;
import com.cinema.hub.backend.dto.promotion.PromotionRequestDto;
import com.cinema.hub.backend.dto.promotion.PromotionResponseDto;
import com.cinema.hub.backend.dto.promotion.PromotionSummaryDto;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface PromotionService {

    PageResponse<PromotionResponseDto> search(String keyword,
                                              Boolean active,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              Pageable pageable);

    PromotionResponseDto getById(Long id);

    PromotionResponseDto create(PromotionRequestDto requestDto);

    PromotionResponseDto update(Long id, PromotionRequestDto requestDto);

    void delete(Long id);

    PageResponse<PromotionSummaryDto> getPublicPromotions(Pageable pageable);

    PromotionDetailDto getDetailBySlug(String slug);

    List<PromotionSummaryDto> getActiveOptions();
}
