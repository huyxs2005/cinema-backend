package com.cinema.hub.backend.service.impl;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.promotion.PromotionDetailDto;
import com.cinema.hub.backend.dto.promotion.PromotionRequestDto;
import com.cinema.hub.backend.dto.promotion.PromotionResponseDto;
import com.cinema.hub.backend.dto.promotion.PromotionSummaryDto;
import com.cinema.hub.backend.entity.Promotion;
import com.cinema.hub.backend.mapper.PromotionMapper;
import com.cinema.hub.backend.repository.PromotionRepository;
import com.cinema.hub.backend.service.PromotionService;
import com.cinema.hub.backend.specification.PromotionSpecifications;
import jakarta.persistence.EntityNotFoundException;
import java.text.Normalizer;
import java.time.LocalDate;
import com.cinema.hub.backend.util.TimeProvider;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionResponseDto> search(String keyword,
                                                     Boolean active,
                                                     LocalDate fromDate,
                                                     LocalDate toDate,
                                                     Pageable pageable) {
        Specification<Promotion> spec = Specification.where(PromotionSpecifications.hasKeyword(keyword))
                .and(PromotionSpecifications.hasStatus(active))
                .and(PromotionSpecifications.publishedFrom(fromDate))
                .and(PromotionSpecifications.publishedTo(toDate));
        Page<Promotion> page = promotionRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(PromotionMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionResponseDto getById(Long id) {
        return PromotionMapper.toResponse(findByIdOrThrow(id));
    }

    @Override
    @Transactional
    public PromotionResponseDto create(PromotionRequestDto requestDto) {
        Promotion promotion = Promotion.builder().build();
        apply(requestDto, promotion);
        promotion.setSlug(generateUniqueSlug(requestDto.getTitle(), null));
        Promotion saved = promotionRepository.save(promotion);
        return PromotionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponseDto update(Long id, PromotionRequestDto requestDto) {
        Promotion promotion = findByIdOrThrow(id);
        boolean titleChanged = StringUtils.hasText(requestDto.getTitle())
                && !requestDto.getTitle().equalsIgnoreCase(promotion.getTitle());
        apply(requestDto, promotion);
        if (titleChanged) {
            promotion.setSlug(generateUniqueSlug(promotion.getTitle(), promotion.getId()));
        }
        Promotion saved = promotionRepository.save(promotion);
        return PromotionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Promotion promotion = findByIdOrThrow(id);
        promotionRepository.delete(promotion);
    }

    @Override
    public PromotionResponseDto updateActiveStatus(Long id, boolean active) {
        Promotion promotion = findByIdOrThrow(id);
        promotion.setIsActive(active);
        Promotion saved = promotionRepository.save(promotion);
        return PromotionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionSummaryDto> getPublicPromotions(Pageable pageable) {
        Specification<Promotion> spec = Specification.where(PromotionSpecifications.hasStatus(true))
                .and(PromotionSpecifications.publishedUpTo(LocalDate.now(TimeProvider.VN_ZONE_ID)));
        Page<Promotion> page = promotionRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(PromotionMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionDetailDto getDetailBySlug(String slug) {
        Promotion promotion = promotionRepository.findBySlugIgnoreCase(slug)
                .filter(Promotion::getIsActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found"));
        if (promotion.getPublishedDate() != null && promotion.getPublishedDate().isAfter(LocalDate.now(TimeProvider.VN_ZONE_ID))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not available yet");
        }
        return PromotionMapper.toDetail(promotion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionSummaryDto> getActiveOptions() {
        Specification<Promotion> spec = Specification.where(PromotionSpecifications.hasStatus(true))
                .and(PromotionSpecifications.publishedUpTo(LocalDate.now(TimeProvider.VN_ZONE_ID)));
        Sort sort = Sort.by(Sort.Order.asc("title"));
        return promotionRepository.findAll(spec, sort).stream()
                .map(PromotionMapper::toSummary)
                .toList();
    }

    private Promotion findByIdOrThrow(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Promotion not found"));
    }

    private void apply(PromotionRequestDto dto, Promotion entity) {
        entity.setTitle(dto.getTitle());
        entity.setThumbnailUrl(dto.getThumbnailUrl());
        entity.setContent(dto.getContent());
        entity.setImgContentUrl(dto.getImgContentUrl());
        entity.setPublishedDate(dto.getPublishedDate() != null ? dto.getPublishedDate() : LocalDate.now(TimeProvider.VN_ZONE_ID));
        entity.setIsActive(dto.getActive());
    }

    private String generateUniqueSlug(String title, Long currentId) {
        String base = slugify(title);
        if (!StringUtils.hasText(base)) {
            base = "promo";
        }
        String candidate = base;
        int counter = 2;
        while (true) {
            Optional<Promotion> existing = promotionRepository.findBySlugIgnoreCase(candidate);
            if (existing.isEmpty() || (currentId != null && existing.get().getId().equals(currentId))) {
                return candidate;
            }
            candidate = base + "-" + counter++;
        }
    }

    private String slugify(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[^\\w\\s-]", " ");
        normalized = normalized.replaceAll("[\\s-_]+", "-");
        normalized = normalized.replaceAll("^-+|-+$", "");
        return normalized.toLowerCase(Locale.ROOT);
    }
}
