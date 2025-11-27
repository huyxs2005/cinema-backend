package com.cinema.hub.backend.service.impl;

import com.cinema.hub.backend.dto.HomeBannerAdminRequestDto;
import com.cinema.hub.backend.dto.HomeBannerResponseDto;
import com.cinema.hub.backend.entity.HomeBanner;
import com.cinema.hub.backend.repository.HomeBannerRepository;
import com.cinema.hub.backend.repository.MovieRepository;
import com.cinema.hub.backend.service.HomeBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeBannerServiceImpl implements HomeBannerService {

    private final HomeBannerRepository homeBannerRepository;
    private final MovieRepository movieRepository;

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
    public List<HomeBannerResponseDto> getAllBannersForAdmin() {
        refreshBannerStatuses();
        Sort sort = Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("id"));
        return homeBannerRepository.findAll(sort).stream()
                .map(this::mapToResponseDto)
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
        return HomeBannerResponseDto.builder()
                .id(banner.getId())
                .imagePath(banner.getImagePath())
                .linkType(banner.getLinkType())
                .movieId(banner.getMovieId())
                .movieTitle(resolveMovieTitle(banner.getMovieId()))
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
        banner.setMovieId(dto.getMovieId());
        banner.setTargetUrl(dto.getTargetUrl());
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

    private String resolveMovieTitle(Integer movieId) {
        if (movieId == null) {
            return null;
        }
        return movieRepository.findById(movieId)
                .map(movie -> movie.getTitle())
                .orElse(null);
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
}
