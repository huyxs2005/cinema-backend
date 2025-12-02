package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.showtime.ShowtimeRequest;
import com.cinema.hub.backend.dto.showtime.ShowtimeResponse;
import com.cinema.hub.backend.service.ShowtimeService;
import com.cinema.hub.backend.util.PaginationUtil;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/admin/showtimes")
@Validated
public class ShowtimeAdminController {

    private final ShowtimeService showtimeService;

    public ShowtimeAdminController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @PostMapping
    public ResponseEntity<List<ShowtimeResponse>> create(@Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.ok(showtimeService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> update(@PathVariable int id,
                                                   @Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.ok(showtimeService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeResponse> get(@PathVariable int id) {
        return ResponseEntity.ok(showtimeService.get(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable int id) {
        showtimeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<PageResponse<ShowtimeResponse>> search(@RequestParam(required = false) Integer movieId,
                                                                 @RequestParam(required = false) Integer auditoriumId,
                                                                 @RequestParam(required = false) Boolean active,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                 LocalDate fromDate,
                                                                 @RequestParam(required = false)
                                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                 LocalDate toDate,
                                                                 @RequestParam(required = false) String keyword,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size,
                                                                 @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtil.create(page, size, sort);
        return ResponseEntity.ok(
                showtimeService.search(movieId, auditoriumId, active, fromDate, toDate, keyword, pageable));
    }
}
