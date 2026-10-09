package com.wildlife.wildlife_conservationbackend.utility;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class PasswordVerifier {
    private static final int MAX_PASSWORD_BYTES = 72;

    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public boolean matches(String password, String storedHash) {
        String hash = storedHash == null ? dummyPasswordHash : storedHash;
        boolean passwordMatches = password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES
                && passwordEncoder.matches(password, hash);
        return storedHash != null && passwordMatches;
    }
}
