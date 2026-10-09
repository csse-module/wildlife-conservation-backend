package com.wildlife.wildlife_conservationbackend.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("wildlife.jwt")
public record JwtProperties(@NotBlank String issuer, @NotBlank String secret,
                            @Min(60) @Max(86400) long ttlSeconds) {
    @Override
    public String toString() {
        return "JwtProperties[secret=REDACTED]";
    }
}
