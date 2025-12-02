package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.snack.SnackRequestDto;
import com.cinema.hub.backend.dto.snack.SnackResponseDto;
import com.cinema.hub.backend.service.SnackService;
import com.cinema.hub.backend.util.PaginationUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/snacks")
@RequiredArgsConstructor
public class SnackAdminController {

    private final SnackService snackService;

    @GetMapping
    public PageResponse<SnackResponseDto> search(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) Boolean available,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtil.create(page, size, sort);
        return snackService.search(keyword, category, available, pageable);
    }

    @GetMapping("/{id}")
    public SnackResponseDto get(@PathVariable Long id) {
        return snackService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SnackResponseDto create(@Valid @RequestBody SnackRequestDto request) {
        return snackService.create(request);
    }

    @PutMapping("/{id}")
    public SnackResponseDto update(@PathVariable Long id,
                                   @Valid @RequestBody SnackRequestDto request) {
        return snackService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        snackService.delete(id);
    }

    @GetMapping("/options")
    public List<SnackResponseDto> activeSnacks() {
        return snackService.listActive();
    }
}
