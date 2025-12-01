package com.cinema.hub.backend.service;

import com.cinema.hub.backend.entity.Role;
import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.repository.RoleRepository;
import com.cinema.hub.backend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.cinema.hub.backend.security.UserPrincipal;
import com.cinema.hub.backend.util.TimeProvider;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Optional<UserAccount> findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return userAccountRepository.findByEmailIgnoreCase(email.trim());
    }

    @Transactional
    public UserAccount findOrCreateGuest(String fullName, String email, String phone) {
        return findByEmail(email)
                .map(existing -> updateContactInfo(existing, fullName, phone))
                .orElseGet(() -> registerGuest(fullName, email, phone));
    }

    @Transactional(readOnly = true)
    public Optional<UserAccount> getById(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return userAccountRepository.findById(id);
    }

    private UserAccount updateContactInfo(UserAccount user, String fullName, String phone) {
        boolean updated = false;
        if (StringUtils.hasText(fullName) && !fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
            updated = true;
        }
        if (StringUtils.hasText(phone) && (user.getPhone() == null || !phone.equals(user.getPhone()))) {
            user.setPhone(phone);
            updated = true;
        }
        if (updated) {
            return userAccountRepository.save(user);
        }
        return user;
    }

    private UserAccount registerGuest(String fullName, String email, String phone) {
        UserAccount user = UserAccount.builder()
                .fullName(StringUtils.hasText(fullName) ? fullName.trim() : "Guest " + UUID.randomUUID())
                .email(email.trim().toLowerCase())
                .phone(StringUtils.hasText(phone) ? phone.trim() : null)
                .passwordHash(UUID.randomUUID().toString())
                .role(resolveDefaultRole())
                .active(true)
                .createdAt(TimeProvider.now())
                .build();
        return userAccountRepository.save(user);
    }

    private Role resolveDefaultRole() {
        return roleRepository.findByNameIgnoreCase("User")
                .orElseThrow(() -> new IllegalStateException("Default user role is missing"));
    }

    public UserAccount requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Bạn cần đăng nhập để tiếp tục.");
        }
        return principal.getUser();
    }
}
