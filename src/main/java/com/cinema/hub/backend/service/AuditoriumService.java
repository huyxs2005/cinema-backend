package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.auditorium.AuditoriumRequest;
import com.cinema.hub.backend.dto.auditorium.AuditoriumResponse;
import com.cinema.hub.backend.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AuditoriumService {

    AuditoriumResponse create(AuditoriumRequest request);

    AuditoriumResponse update(int id, AuditoriumRequest request);

    AuditoriumResponse get(int id);

    void delete(int id);

    PageResponse<AuditoriumResponse> search(String name, Boolean active, Pageable pageable);
}
