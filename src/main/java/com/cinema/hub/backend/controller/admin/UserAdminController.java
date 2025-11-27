package com.cinema.hub.backend.controller.admin;

import com.cinema.hub.backend.dto.admin.user.UserAdminRequestDto;
import com.cinema.hub.backend.dto.admin.user.UserAdminResponseDto;
import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.service.UserAdminService;
import com.cinema.hub.backend.util.PaginationUtil;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import com.cinema.hub.backend.security.UserPrincipal;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserAdminResponseDto>> search(@RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) String role,
                                                                     @RequestParam(required = false) Boolean active,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "20") int size,
                                                                     @RequestParam(required = false) String sort) {
        Pageable pageable = PaginationUtil.create(page, size, sort);
        return ResponseEntity.ok(userAdminService.search(keyword, role, active, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAdminResponseDto> get(@PathVariable int id) {
        return ResponseEntity.ok(userAdminService.get(id));
    }

    @PostMapping
    public ResponseEntity<UserAdminResponseDto> create(@Valid @RequestBody UserAdminRequestDto request) {
        return ResponseEntity.ok(userAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAdminResponseDto> update(@PathVariable int id,
                                                       @Valid @RequestBody UserAdminRequestDto request) {
        return ResponseEntity.ok(userAdminService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable int id,
                                             @RequestParam boolean active,
                                             Authentication authentication) {
        if (!active && isCurrentUser(authentication, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể vô hiệu hóa tài khoản đang đăng nhập.");
        }
        userAdminService.updateStatus(id, active);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id, Authentication authentication) {
        if (isCurrentUser(authentication, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa tài khoản đang đăng nhập.");
        }
        userAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isCurrentUser(Authentication authentication, int targetId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.getUser() != null
                && principal.getUser().getId() != null
                && principal.getUser().getId() == targetId;
    }
}
