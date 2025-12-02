package com.cinema.hub.backend.config;

import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewAttributes {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }

    @ModelAttribute("currentUser")
    public UserAccount currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return principal != null ? principal.getUser() : null;
    }

    @ModelAttribute("currentUserPayload")
    public Map<String, Object> currentUserPayload(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        UserAccount user = principal.getUser();
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", user.getId());
        payload.put("fullName", user.getFullName());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole() != null ? user.getRole().getName() : null);
        payload.put("phone", user.getPhone());
        return payload;
    }
}
