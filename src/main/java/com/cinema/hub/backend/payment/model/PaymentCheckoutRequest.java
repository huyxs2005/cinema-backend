package com.cinema.hub.backend.payment.model;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class PaymentCheckoutRequest {

    private String holdToken;

    private Integer bookingId;

    private String fullName;

    private String phone;

    @NotBlank
    @Email
    private String email;

    @AssertTrue(message = "Cần cung cấp holdToken hoặc bookingId")
    public boolean isTargetProvided() {
        return StringUtils.hasText(holdToken) || bookingId != null;
    }
}
