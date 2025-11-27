package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.dto.HomeBannerAdminRequestDto;
import com.cinema.hub.backend.dto.HomeBannerResponseDto;
import com.cinema.hub.backend.service.HomeBannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
@RequiredArgsConstructor
public class HomeBannerAdminController {

    private final HomeBannerService homeBannerService;

    @GetMapping
    public List<HomeBannerResponseDto> getAllBanners() {
        return homeBannerService.getAllBannersForAdmin();
    }

    @GetMapping("/{id}")
    public HomeBannerResponseDto getBanner(@PathVariable int id) {
        return homeBannerService.getBannerById(id);
    }

    @PostMapping
    public HomeBannerResponseDto createBanner(@Valid @RequestBody HomeBannerAdminRequestDto requestDto) {
        return homeBannerService.createBanner(requestDto);
    }

    @PutMapping("/{id}")
    public HomeBannerResponseDto updateBanner(@PathVariable int id,
                                              @Valid @RequestBody HomeBannerAdminRequestDto requestDto) {
        return homeBannerService.updateBanner(id, requestDto);
    }

    @DeleteMapping("/{id}")
    public void deleteBanner(@PathVariable int id) {
        homeBannerService.deleteBanner(id);
    }
}
