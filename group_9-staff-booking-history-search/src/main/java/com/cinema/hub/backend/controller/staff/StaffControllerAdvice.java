package com.cinema.hub.backend.controller.staff;

import com.cinema.hub.backend.entity.UserAccount;
import com.cinema.hub.backend.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {
        StaffAuthController.class,
        StaffDashboardController.class,
        StaffBookingController.class
})
public class StaffControllerAdvice {

    @ModelAttribute("currentStaff")
    public UserAccount injectStaff(@AuthenticationPrincipal UserPrincipal principal) {
        return principal != null ? principal.getUser() : null;
    }
}
