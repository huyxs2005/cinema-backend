package com.cinema.hub.backend.controller;

import com.cinema.hub.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/uploads")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadBannerImage(@RequestParam("file") MultipartFile file) {
        try {
            String path = fileStorageService.storeBannerImage(file);
            return Map.of("path", path);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kh?ng th? t?i l?n ?nh", e);
        }
    }

    @PostMapping(value = "/movie-poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadMoviePoster(@RequestParam("file") MultipartFile file) {
        try {
            String path = fileStorageService.storeMoviePoster(file);
            return Map.of("path", path);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kh?ng th? t?i l?n poster", e);
        }
    }
}

