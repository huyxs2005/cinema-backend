package com.cinema.hub.backend.service;

import com.cinema.hub.backend.dto.admin.user.UserAdminRequestDto;
import com.cinema.hub.backend.dto.admin.user.UserAdminResponseDto;
import com.cinema.hub.backend.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UserAdminService {

    UserAdminResponseDto create(UserAdminRequestDto request);

    UserAdminResponseDto update(int id, UserAdminRequestDto request);

    UserAdminResponseDto get(int id);

    void updateStatus(int id, boolean active);

    void delete(int id);

    PageResponse<UserAdminResponseDto> search(String keyword, String role, Boolean active, Pageable pageable);
}
