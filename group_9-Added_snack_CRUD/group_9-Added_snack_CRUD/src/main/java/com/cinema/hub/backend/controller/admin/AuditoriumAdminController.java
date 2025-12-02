package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.dto.auditorium.AuditoriumRequest;
import com.cinema.hub.backend.dto.auditorium.AuditoriumResponse;
import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.service.AuditoriumService;
import com.cinema.hub.backend.util.PaginationUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auditoriums")
@Validated
public class AuditoriumAdminController {

    private final AuditoriumService auditoriumService;

    public AuditoriumAdminController(AuditoriumService auditoriumService) {
        this.auditoriumService = auditoriumService;
    }

    @PostMapping
    public ResponseEntity<AuditoriumResponse> create(@Valid @RequestBody AuditoriumRequest request) {
        return ResponseEntity.ok(auditoriumService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> update(@PathVariable int id,
                                                     @Valid @RequestBody AuditoriumRequest request) {
        return ResponseEntity.ok(auditoriumService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriumResponse> get(@PathVariable int id) {
        return ResponseEntity.ok(auditoriumService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        auditoriumService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<AuditoriumResponse>> search(@RequestParam(required = false) String name,
                                                                   @RequestParam(required = false) Boolean active,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size,
                                                                   @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtil.create(page, size, sort);
        return ResponseEntity.ok(auditoriumService.search(name, active, pageable));
    }
}
