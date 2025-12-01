package com.cinema.hub.backend.payment.payos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app.payos")
@Validated
@Getter
@Setter
public class PayOSConfig {

    @NotBlank
    private String clientId;

    @NotBlank
    private String apiKey;

    @NotBlank
    private String checksumKey;

    private String apiBaseUrl = "https://api-merchant.payos.vn";
}
