package com.cinema.hub.backend.service.impl;

import com.cinema.hub.backend.dto.admin.user.UserAdminRequestDto;
import com.cinema.hub.backend.dto.admin.user.UserAdminResponseDto;
import com.cinema.hub.backend.dto.common.PageResponse;
import com.cinema.hub.backend.entity.Role;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.repository.RoleRepository;
import com.cinema.hub.backend.repository.UserAccountRepository;
import com.cinema.hub.backend.service.UserAdminService;
import com.cinema.hub.backend.specification.UserAccountSpecifications;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private static final String ADMIN_ROLE = "Admin";
    private static final String ADMIN_GUARD_MESSAGE = "Phải còn ít nhất một quản trị viên đang hoạt động.";

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminServiceImpl(UserAccountRepository userAccountRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserAdminResponseDto create(UserAdminRequestDto request) {
        validatePasswordRequired(request.getPassword());
        String normalizedEmail = request.getEmail().trim();
        if (userAccountRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng");
        }
        String normalizedPhone = normalizePhone(request.getPhone());
        if (userAccountRepository.findByPhone(normalizedPhone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại đã được sử dụng");
        }
        Role role = findRole(request.getRole());
        UserAccount user = UserAccount.builder()
                .email(normalizedEmail)
                .fullName(request.getFullName())
                .phone(normalizedPhone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(request.getActive() == null || request.getActive())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        return toDto(userAccountRepository.save(user));
    }

    @Override
    public UserAdminResponseDto update(int id, UserAdminRequestDto request) {
        UserAccount user = getEntity(id);

        String normalizedEmail = request.getEmail().trim();
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)
                && userAccountRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng");
        }

        String normalizedPhone = normalizePhone(request.getPhone());
        userAccountRepository.findByPhone(normalizedPhone)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại đã được sử dụng");
                });

        Role role = findRole(request.getRole());
        boolean targetActive = request.getActive() != null ? request.getActive() : user.isActive();
        ensureActiveAdminRemains(user, targetActive, role);

        user.setEmail(normalizedEmail);
        user.setFullName(request.getFullName());
        user.setPhone(normalizedPhone);
        user.setRole(role);
        user.setActive(targetActive);
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        return toDto(userAccountRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminResponseDto get(int id) {
        return toDto(getEntity(id));
    }

    @Override
    public void updateStatus(int id, boolean active) {
        UserAccount user = getEntity(id);
        ensureActiveAdminRemains(user, active, user.getRole());
        user.setActive(active);
        userAccountRepository.save(user);
    }

    @Override
    public void delete(int id) {
        UserAccount user = getEntity(id);
        ensureActiveAdminRemains(user, false, user.getRole());
        userAccountRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserAdminResponseDto> search(String keyword, String role, Boolean active, Pageable pageable) {
        Page<UserAccount> page = userAccountRepository.findAll(
                UserAccountSpecifications.filter(keyword, role, active), pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    private Role findRole(String roleName) {
        return roleRepository.findByNameIgnoreCase(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy vai trò: " + roleName));
    }

    private UserAccount getEntity(int id) {
        return userAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng: " + id));
    }

    private void validatePasswordRequired(String password) {
        if (!StringUtils.hasText(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không được để trống");
        }
        if (password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải từ 8 ký tự trở lên");
        }
    }

    private String normalizePhone(String rawPhone) {
        String digits = rawPhone == null ? "" : rawPhone.replaceAll("[^0-9]", "");
        if (digits.length() < 10 || digits.length() > 11) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không hợp lệ (10-11 chữ số)");
        }
        return digits;
    }

    private UserAdminResponseDto toDto(UserAccount user) {
        return UserAdminResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().getName() : "User")
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private void ensureActiveAdminRemains(UserAccount user, boolean targetActive, Role targetRole) {
        boolean currentlyAdminActive = user.isActive() && isAdminRole(user.getRole());
        boolean willBeAdminActive = targetActive && isAdminRole(targetRole);
        if (currentlyAdminActive && !willBeAdminActive) {
            long activeAdmins = userAccountRepository.countByRole_NameIgnoreCaseAndActiveTrue(ADMIN_ROLE);
            if (activeAdmins <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ADMIN_GUARD_MESSAGE);
            }
        }
    }

    private boolean isAdminRole(Role role) {
        return role != null && ADMIN_ROLE.equalsIgnoreCase(role.getName());
    }
}
