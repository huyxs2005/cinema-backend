package com.cinema.hub.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeBannerImage(MultipartFile file) throws IOException {
        return storeFile(file, "banners");
    }

    public String storeMoviePoster(MultipartFile file) throws IOException {
        return storeFile(file, "movies");
    }

    public String storeMovieTrailer(MultipartFile file) throws IOException {
        return storeFile(file, "trailers");
    }

    private String storeFile(MultipartFile file, String subDirectory) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex >= 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String newFilename = UUID.randomUUID() + extension;
        Path targetDir = uploadRoot.resolve(subDirectory);
        Files.createDirectories(targetDir);

        Path target = targetDir.resolve(newFilename).normalize();
        file.transferTo(target.toFile());

        return "/uploads/" + subDirectory + "/" + newFilename.replace("\\", "/");
    }
}
