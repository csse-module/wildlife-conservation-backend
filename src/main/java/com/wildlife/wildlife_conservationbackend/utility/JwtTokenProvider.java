package com.wildlife.wildlife_conservationbackend.utility;

import com.wildlife.wildlife_conservationbackend.config.JwtProperties;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public String issue(UserEntity user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.issuer()).subject(user.getId())
                .issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(expiresIn()))
                .id(UUID.randomUUID().toString()).claim("tokenVersion", user.getTokenVersion()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public long expiresIn() {
        return properties.ttlSeconds();
    }
}
