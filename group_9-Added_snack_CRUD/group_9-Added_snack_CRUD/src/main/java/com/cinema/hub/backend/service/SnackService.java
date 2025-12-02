package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.dto.snack.SnackRequestDto;
import com.cinema.hub.backend.dto.snack.SnackResponseDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface SnackService {

    PageResponse<SnackResponseDto> search(String keyword,
                                          String category,
                                          Boolean available,
                                          Pageable pageable);

    SnackResponseDto getById(Long id);

    SnackResponseDto create(SnackRequestDto request);

    SnackResponseDto update(Long id, SnackRequestDto request);

    void delete(Long id);

    List<SnackResponseDto> listActive();
}
