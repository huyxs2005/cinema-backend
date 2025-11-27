package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffAuthController {

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null && principal.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_STAFF".equalsIgnoreCase(auth.getAuthority()))) {
            return "redirect:/staff/dashboard";
        }
        return "staff/login";
    }
}
