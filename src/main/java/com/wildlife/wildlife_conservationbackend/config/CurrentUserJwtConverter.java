package com.wildlife.wildlife_conservationbackend.config;

import com.wildlife.wildlife_conservationbackend.domain.CurrentUser;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
        }
        UserEntity user;
        try {
            user = userRepository.findById(jwt.getSubject()).filter(UserEntity::isActive)
                    .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error("invalid_token")));
        } catch (DataAccessException exception) {
            throw new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable"), exception);
        }
        requireCurrentToken(jwt, user);
        var principal = new CurrentUser(user.getId(), user.getRole(), Set.copyOf(user.getParkIds()));
        String passwordAuthority = user.isPasswordChangeRequired()
                ? AccountAuthorities.PASSWORD_CHANGE_REQUIRED : AccountAuthorities.PASSWORD_READY;
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()), new SimpleGrantedAuthority(passwordAuthority)));
    }

    private void requireCurrentToken(Jwt jwt, UserEntity user) {
        Object version = jwt.getClaim("tokenVersion");
        boolean matches = version == null ? user.getTokenVersion() == 0
                : version instanceof Number number && number.longValue() == user.getTokenVersion();
        if (!matches) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"));
        }
    }
}
