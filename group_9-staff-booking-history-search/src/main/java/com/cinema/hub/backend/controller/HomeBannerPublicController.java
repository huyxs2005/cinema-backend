package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.HomeBannerResponseDto;
import com.cinema.hub.backend.service.HomeBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class HomeBannerPublicController {

    private final HomeBannerService homeBannerService;

    @GetMapping("/active")
    public List<HomeBannerResponseDto> getActiveBanners() {
        return homeBannerService.getActiveBannersForHomepage();
    }
}
