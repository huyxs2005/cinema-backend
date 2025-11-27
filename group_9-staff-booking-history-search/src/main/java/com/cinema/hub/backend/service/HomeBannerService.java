package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.HomeBannerAdminRequestDto;
import com.cinema.hub.backend.dto.HomeBannerResponseDto;

import java.util.List;

public interface HomeBannerService {

    List<HomeBannerResponseDto> getActiveBannersForHomepage();

    List<HomeBannerResponseDto> getAllBannersForAdmin();

    HomeBannerResponseDto getBannerById(int id);

    HomeBannerResponseDto createBanner(HomeBannerAdminRequestDto dto);

    HomeBannerResponseDto updateBanner(int id, HomeBannerAdminRequestDto dto);

    void deleteBanner(int id);
}
