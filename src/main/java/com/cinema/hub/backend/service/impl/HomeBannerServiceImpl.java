package com.cinema.hub.backend.service.impl;

import com.cinema.hub.backend.dto.HomeBannerAdminRequestDto;
import com.cinema.hub.backend.dto.HomeBannerResponseDto;
import com.cinema.hub.backend.entity.HomeBanner;
import com.cinema.hub.backend.entity.Promotion;
import com.cinema.hub.backend.repository.HomeBannerRepository;
import com.cinema.hub.backend.repository.MovieRepository;
import com.cinema.hub.backend.repository.PromotionRepository;
import com.cinema.hub.backend.service.HomeBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HomeBannerServiceImpl implements HomeBannerService {

    private final HomeBannerRepository homeBannerRepository;
    private final MovieRepository movieRepository;
    private final PromotionRepository promotionRepository;

    @Override
    public List<HomeBannerResponseDto> getActiveBannersForHomepage() {
        refreshBannerStatuses();
        LocalDate today = LocalDate.now();
        return homeBannerRepository.findByIsActiveTrueOrderBySortOrderAsc().stream()
                .filter(banner -> isWithinActiveWindow(banner, today))
                .map(this::mapToResponseDto)
                .toList();
    }

    @Override
    public List<HomeBannerResponseDto> getAllBannersForAdmin(String keyword, Boolean active) {
        refreshBannerStatuses();
        Sort sort = Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"));
        List<HomeBannerResponseDto> banners = homeBannerRepository.findAll(sort).stream()
                .map(this::mapToResponseDto)
                .toList();
        String normalizedKeyword = optionalTrimLower(keyword);
        return banners.stream()
                .filter(banner -> matchesKeyword(normalizedKeyword, banner))
                .filter(banner -> matchesStatus(active, banner))
                .toList();
    }

    @Override
    public HomeBannerResponseDto getBannerById(int id) {
        HomeBanner banner = findBannerOrThrow(id);
        return mapToResponseDto(banner);
    }

    @Override
    public HomeBannerResponseDto createBanner(HomeBannerAdminRequestDto dto) {
        HomeBanner banner = HomeBanner.builder().build();
        applyRequestToEntity(dto, banner);
        HomeBanner saved = homeBannerRepository.save(banner);
        refreshBannerStatuses();
        return mapToResponseDto(findBannerOrThrow(saved.getId()));
    }

    @Override
    public HomeBannerResponseDto updateBanner(int id, HomeBannerAdminRequestDto dto) {
        HomeBanner banner = findBannerOrThrow(id);
        applyRequestToEntity(dto, banner);
        HomeBanner saved = homeBannerRepository.save(banner);
        refreshBannerStatuses();
        return mapToResponseDto(findBannerOrThrow(saved.getId()));
    }

    @Override
    public void deleteBanner(int id) {
        HomeBanner banner = findBannerOrThrow(id);
        homeBannerRepository.delete(banner);
        refreshBannerStatuses();
    }

    private boolean isWithinActiveWindow(HomeBanner banner, LocalDate today) {
        boolean startsBeforeToday = banner.getStartDate() == null || !banner.getStartDate().isAfter(today);
        boolean endsAfterToday = banner.getEndDate() == null || !banner.getEndDate().isBefore(today);
        return startsBeforeToday && endsAfterToday;
    }

    private HomeBannerResponseDto mapToResponseDto(HomeBanner banner) {
        Promotion promotion = fetchPromotion(banner.getPromotionId());
        return HomeBannerResponseDto.builder()
                .id(banner.getId())
                .imagePath(banner.getImagePath())
                .linkType(banner.getLinkType())
                .movieId(banner.getMovieId())
                .movieTitle(resolveMovieTitle(banner.getMovieId()))
                .promotionId(banner.getPromotionId())
                .promotionSlug(promotion != null ? promotion.getSlug() : null)
                .promotionTitle(promotion != null ? promotion.getTitle() : null)
                .targetUrl(banner.getTargetUrl())
                .sortOrder(banner.getSortOrder())
                .isActive(banner.getIsActive())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .build();
    }

    private void applyRequestToEntity(HomeBannerAdminRequestDto dto, HomeBanner banner) {
        banner.setImagePath(dto.getImagePath());
        banner.setLinkType(dto.getLinkType());
        banner.setMovieId("MOVIE".equalsIgnoreCase(dto.getLinkType()) ? dto.getMovieId() : null);
        banner.setPromotionId(resolvePromotionAssignment(dto));
        banner.setTargetUrl("URL".equalsIgnoreCase(dto.getLinkType()) ? dto.getTargetUrl() : null);
        Integer desiredOrder = dto.getSortOrder() == null
                ? 1
                : Math.min(100, Math.max(1, dto.getSortOrder()));
        banner.setSortOrder(desiredOrder);
        banner.setIsActive(dto.getIsActive());
        banner.setStartDate(dto.getStartDate());
        banner.setEndDate(dto.getEndDate());
    }

    private HomeBanner findBannerOrThrow(int id) {
        return homeBannerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Banner not found"));
    }

    private Long resolvePromotionAssignment(HomeBannerAdminRequestDto dto) {
        if (!"PROMO".equalsIgnoreCase(dto.getLinkType())) {
            return null;
        }
        if (dto.getPromotionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion reference is required");
        }
        boolean exists = promotionRepository.existsById(dto.getPromotionId());
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promotion not found");
        }
        return dto.getPromotionId();
    }

    private String resolveMovieTitle(Integer movieId) {
        if (movieId == null) {
            return null;
        }
        return movieRepository.findById(movieId)
                .map(movie -> movie.getTitle())
                .orElse(null);
    }

    private Promotion fetchPromotion(Long promotionId) {
        if (promotionId == null) {
            return null;
        }
        return promotionRepository.findById(promotionId).orElse(null);
    }

    private void refreshBannerStatuses() {
        LocalDate today = LocalDate.now();
        List<HomeBanner> activeBanners = homeBannerRepository.findByIsActiveTrueOrderBySortOrderAsc();
        boolean updated = false;
        for (HomeBanner banner : activeBanners) {
            if (banner.getEndDate() != null && banner.getEndDate().isBefore(today)) {
                banner.setIsActive(false);
                updated = true;
            }
        }
        if (updated) {
            homeBannerRepository.saveAll(activeBanners);
        }
    }

    private boolean matchesKeyword(String normalizedKeyword, HomeBannerResponseDto banner) {
        if (normalizedKeyword == null) {
            return true;
        }
        return containsIgnoreCase(banner.getMovieTitle(), normalizedKeyword)
                || containsIgnoreCase(banner.getPromotionTitle(), normalizedKeyword)
                || containsIgnoreCase(banner.getPromotionSlug(), normalizedKeyword)
                || containsIgnoreCase(banner.getTargetUrl(), normalizedKeyword)
                || containsIgnoreCase(banner.getImagePath(), normalizedKeyword);
    }

    private boolean containsIgnoreCase(String value, String normalized) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalized);
    }

    private boolean matchesStatus(Boolean activeFilter, HomeBannerResponseDto banner) {
        if (activeFilter == null) {
            return true;
        }
        return Boolean.valueOf(activeFilter).equals(banner.getIsActive());
    }

    private String optionalTrimLower(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }
}
