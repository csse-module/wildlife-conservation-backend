package com.wildlife.wildlife_conservationbackend.service.impl;

import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class AccountCreator {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserEntity create(String name, String email, String password, Role role,
                             Set<String> parkIds, boolean passwordChangeRequired) {
        if (userRepository.findByNormalizedEmail(email).isPresent()) {
            throw duplicateEmail();
        }
        UserEntity user = UserEntity.builder().id(UUID.randomUUID().toString()).name(name).normalizedEmail(email)
                .passwordHash(passwordEncoder.encode(password)).role(role).parkIds(Set.copyOf(parkIds))
                .active(true).passwordChangeRequired(passwordChangeRequired).build();
        try {
            return userRepository.insert(user);
        } catch (DuplicateKeyException exception) {
            throw duplicateEmail();
        }
    }

    private ApiException duplicateEmail() {
        return ApiException.conflict("EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
    }
}
