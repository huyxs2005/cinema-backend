package com.cinema.hub.backend.service.impl;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.snack.SnackRequestDto;
import com.cinema.hub.backend.dto.snack.SnackResponseDto;
import com.cinema.hub.backend.entity.Snack;
import com.cinema.hub.backend.mapper.SnackMapper;
import com.cinema.hub.backend.repository.SnackRepository;
import com.cinema.hub.backend.service.SnackService;
import com.cinema.hub.backend.specification.SnackSpecifications;
import jakarta.persistence.EntityNotFoundException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
public class SnackServiceImpl implements SnackService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of("POPCORN", "DRINK", "SNACK");

    private final SnackRepository snackRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SnackResponseDto> search(String keyword,
                                                 String category,
                                                 Boolean available,
                                                 Pageable pageable) {
        Specification<Snack> spec = Specification.where(SnackSpecifications.hasKeyword(keyword))
                .and(SnackSpecifications.hasCategory(category))
                .and(SnackSpecifications.hasAvailability(available));
        Page<Snack> page = snackRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(SnackMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public SnackResponseDto getById(Long id) {
        return SnackMapper.toResponse(findByIdOrThrow(id));
    }

    @Override
    @Transactional
    public SnackResponseDto create(SnackRequestDto request) {
        Snack snack = new Snack();
        applyRequest(snack, request);
        snack.setSlug(generateSlugFromName(snack.getName(), null));
        Snack saved = snackRepository.save(snack);
        return SnackMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public SnackResponseDto update(Long id, SnackRequestDto request) {
        Snack snack = findByIdOrThrow(id);
        applyRequest(snack, request);
        snack.setSlug(generateSlugFromName(snack.getName(), snack.getId()));
        Snack saved = snackRepository.save(snack);
        return SnackMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Snack snack = findByIdOrThrow(id);
        snackRepository.delete(snack);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SnackResponseDto> listActive() {
        Specification<Snack> spec = SnackSpecifications.hasAvailability(true);
        Sort sort = Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("name"));
        return snackRepository.findAll(spec, sort).stream()
                .map(SnackMapper::toResponse)
                .toList();
    }

    private Snack findByIdOrThrow(Long id) {
        return snackRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Snack not found"));
    }

    private void applyRequest(Snack snack, SnackRequestDto request) {
        snack.setName(request.getName().trim());
        snack.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        snack.setCategory(normalizeCategory(request.getCategory()));
        snack.setPrice(request.getPrice());
        snack.setImageUrl(StringUtils.hasText(request.getImageUrl()) ? request.getImageUrl().trim() : null);
        snack.setServingSize(StringUtils.hasText(request.getServingSize()) ? request.getServingSize().trim() : null);
        if (snack.getStockQuantity() == null) {
            snack.setStockQuantity(0);
        }
        snack.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1);
        snack.setAvailable(request.getAvailable() != null ? request.getAvailable() : Boolean.TRUE);
    }

    private String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thi\u1ebfu category");
        }
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category kh\u00f4ng h\u1ee3p l\u1ec7");
        }
        return normalized;
    }

    private String generateSlugFromName(String baseName, Long currentId) {
        String base = slugify(baseName);
        if (!StringUtils.hasText(base)) {
            base = "snack";
        }
        String candidate = base;
        int counter = 2;
        while (true) {
            Optional<Snack> existing = snackRepository.findBySlugIgnoreCase(candidate);
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
