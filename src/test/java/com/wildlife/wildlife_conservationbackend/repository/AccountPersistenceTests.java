package com.wildlife.wildlife_conservationbackend.repository;

import com.wildlife.wildlife_conservationbackend.config.CurrentUserJwtConverter;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountPersistenceTests {
    @Test
    void existingMongoAccountsRemainUsableWithoutNewFields() throws Exception {
        MongoMappingContext context = new MongoMappingContext();
        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, context);
        converter.afterPropertiesSet();
        context.setSimpleTypeHolder(converter.getCustomConversions().getSimpleTypeHolder());
        context.setInitialEntitySet(Set.of(UserEntity.class));
        context.afterPropertiesSet();
        Document legacy = new Document("_id", "user").append("name", "Ranger")
                .append("normalizedEmail", "ranger@example.com").append("passwordHash", "old-hash")
                .append("role", "RANGER").append("parkIds", List.of("park")).append("active", true);
        UserEntity user = converter.read(UserEntity.class, legacy);
        assertThat(user.isPasswordChangeRequired()).isFalse();
        assertThat(user.getTokenVersion()).isZero();
        user.setPasswordChangeRequired(true);
        user.setTokenVersion(2);
        Document stored = new Document();
        converter.write(user, stored);
        assertThat(stored).containsEntry("passwordChangeRequired", true).containsEntry("tokenVersion", 2L);
        assertThat(converter.read(UserEntity.class, stored)).isEqualTo(user);
    }

    @Test
    void passwordUpdateProtectsActiveAccountHashAndLegacyVersionAtomically() {
        MongoTemplate template = mock(MongoTemplate.class);
        UserEntity user = user();
        new UserPasswordRepository(template).changePassword(user, "new-hash");
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        ArgumentCaptor<FindAndModifyOptions> options = ArgumentCaptor.forClass(FindAndModifyOptions.class);
        verify(template).findAndModify(query.capture(), update.capture(), options.capture(), eq(UserEntity.class));
        Document predicate = query.getValue().getQueryObject();
        assertThat(predicate).containsEntry("_id", "user").containsEntry("active", true).containsEntry("passwordHash", "old-hash");
        List<Document> conditions = predicate.getList("$and", Document.class);
        assertThat(conditions.get(0).getList("$or", Document.class))
                .containsExactly(new Document("tokenVersion", 0), new Document("tokenVersion", new Document("$exists", false)));
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class))
                .containsOnlyKeys("passwordHash", "passwordChangeRequired")
                .containsEntry("passwordHash", "new-hash").containsEntry("passwordChangeRequired", false);
        assertThat(update.getValue().getUpdateObject().get("$inc", Document.class)).containsEntry("tokenVersion", 1);
        assertThat(options.getValue().isReturnNew()).isTrue();
        assertThat(options.getValue().isUpsert()).isFalse();
    }

    @Test
    void subsequentPasswordUpdatesMatchExactVersion() {
        MongoTemplate template = mock(MongoTemplate.class);
        UserEntity user = user();
        user.setTokenVersion(3);
        new UserPasswordRepository(template).changePassword(user, "new-hash");
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(template).findAndModify(query.capture(), any(Update.class), any(FindAndModifyOptions.class), eq(UserEntity.class));
        assertThat(query.getValue().getQueryObject().getList("$and", Document.class))
                .containsExactly(new Document("tokenVersion", 3L));
    }

    @Test
    void converterRejectsOldAndMalformedVersionsAndPreservesLegacyTokensUntilRotation() {
        UserRepository users = mock(UserRepository.class);
        UserEntity user = user();
        when(users.findById("user")).thenReturn(Optional.of(user));
        CurrentUserJwtConverter converter = new CurrentUserJwtConverter(users);
        Jwt legacy = token(null);
        assertThat(converter.convert(legacy).isAuthenticated()).isTrue();
        assertThatThrownBy(() -> converter.convert(token("invalid"))).isInstanceOf(OAuth2AuthenticationException.class);
        user.setTokenVersion(1);
        assertThatThrownBy(() -> converter.convert(legacy)).isInstanceOf(OAuth2AuthenticationException.class);
        assertThatThrownBy(() -> converter.convert(token(0))).isInstanceOf(OAuth2AuthenticationException.class);
        assertThat(converter.convert(token(1)).isAuthenticated()).isTrue();
        user.setActive(false);
        assertThatThrownBy(() -> converter.convert(token(1))).isInstanceOf(OAuth2AuthenticationException.class);
    }

    private UserEntity user() {
        return new UserEntity("user", "Ranger", "ranger@example.com", "old-hash", Role.RANGER, Set.of("park"), true);
    }

    private Jwt token(Object version) {
        Jwt.Builder builder = Jwt.withTokenValue("example").header("alg", "HS256").subject("user")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (version != null) {
            builder.claim("tokenVersion", version);
        }
        return builder.build();
    }
}
