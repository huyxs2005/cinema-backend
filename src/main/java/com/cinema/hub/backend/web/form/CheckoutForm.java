package com.cinema.hub.backend.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class CheckoutForm {

    @NotNull
    @Min(1)
    private Integer showtimeId;

    @NotBlank
    private String seatIds;

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    private String phone;

    private String paymentMethod = "ONLINE";

    private String notes;

    public List<Integer> getSeatIdList() {
        if (!StringUtils.hasText(seatIds)) {
            return List.of();
        }
        return Arrays.stream(seatIds.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
