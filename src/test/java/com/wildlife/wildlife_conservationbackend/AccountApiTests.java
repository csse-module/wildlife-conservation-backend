package com.wildlife.wildlife_conservationbackend;

import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.repository.MongoPageReader;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserPasswordRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import com.wildlife.wildlife_conservationbackend.utility.JwtTokenProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "wildlife.jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
    "spring.data.mongodb.auto-index-creation=false",
    "wildlife.seed.enabled=false",
    "wildlife.registration.community-park-ids=park,missing-park",
    "spring.mongodb.uri=mongodb://localhost:27017/test?serverSelectionTimeoutMS=100&connectTimeoutMS=100"
})
@AutoConfigureMockMvc
class AccountApiTests {
    private static final String PASSWORD = "current-password-01";
    private static final String NEW_PASSWORD = "replacement-password-02";
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtTokenProvider tokens;
    @MockitoBean private UserRepository users;
    @MockitoBean private ParkRepository parks;
    @MockitoBean private MongoPageReader reader;
    @MockitoBean private UserPasswordRepository passwords;
    private final Map<String, UserEntity> accounts = new HashMap<>();

    @BeforeEach
    void fixtures() {
        accounts.clear();
        when(parks.existsById("park")).thenReturn(true);
        when(users.findById(anyString())).thenAnswer(call -> Optional.ofNullable(accounts.get(call.getArgument(0))));
        when(users.findByNormalizedEmail(anyString())).thenAnswer(call -> accounts.values().stream()
                .filter(user -> user.getNormalizedEmail().equals(call.getArgument(0))).findFirst());
        when(users.insert(any(UserEntity.class))).thenAnswer(call -> {
            UserEntity user = call.getArgument(0);
            accounts.put(user.getId(), user);
            return user;
        });
        when(passwords.changePassword(any(UserEntity.class), anyString())).thenAnswer(call -> {
            UserEntity expected = call.getArgument(0);
            UserEntity changed = UserEntity.builder().id(expected.getId()).name(expected.getName())
                    .normalizedEmail(expected.getNormalizedEmail()).passwordHash(call.getArgument(1))
                    .role(expected.getRole()).parkIds(expected.getParkIds()).active(true)
                    .passwordChangeRequired(false).tokenVersion(expected.getTokenVersion() + 1).build();
            accounts.put(changed.getId(), changed);
            return changed;
        });
        when(reader.find(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void publicRegistrationCreatesCommunityAccountThenLoginWorks() throws Exception {
        MvcResult registration = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(registration("Villager@Example.com", PASSWORD, "park"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("00"))
                .andExpect(jsonPath("$.data.name").value("Villager"))
                .andExpect(jsonPath("$.data.email").value("villager@example.com"))
                .andExpect(jsonPath("$.data.role").value("COMMUNITY_MEMBER"))
                .andExpect(jsonPath("$.data.parkIds[0]").value("park"))
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(false))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.tokenVersion").doesNotExist()).andReturn();
        String id = mapper.readTree(registration.getResponse().getContentAsString()).at("/data/id").asText();
        UserEntity stored = accounts.get(id);
        assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
        String token = login("VILLAGER@example.com", PASSWORD);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(id));
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void duplicateEmailCannotOverwriteAnAccount() throws Exception {
        account(Role.RANGER, false);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(registration("RANGER@example.com", NEW_PASSWORD, "park"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.errorCode").value("EMAIL_ALREADY_EXISTS"));
        verify(users, never()).insert(any(UserEntity.class));
        assertThat(encoder.matches(PASSWORD, accounts.get("RANGER").getPasswordHash())).isTrue();
    }

    @Test
    void concurrentDuplicateInsertReturnsConflict() throws Exception {
        when(users.insert(any(UserEntity.class))).thenThrow(new DuplicateKeyException("internal index details"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(registration("member@example.com", PASSWORD, "park"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.errorCode").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.errorDescription").value("An account with this email already exists."));
    }

    @Test
    void unapprovedParkIsRejectedBeforeUserPersistence() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(registration("member@example.com", PASSWORD, "other"))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.errorCode").value("REGISTRATION_NOT_ALLOWED"));
        verify(users, never()).insert(any(UserEntity.class));
    }

    @Test
    void configuredParkMustActuallyExist() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(registration("member@example.com", PASSWORD, "missing-park"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.errorCode").value("VALIDATION_FAILED"));
        verify(users, never()).insert(any(UserEntity.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"role", "active", "passwordChangeRequired", "tokenVersion", "parkIds", "id"})
    void publicRegistrationRejectsServerOwnedFields(String field) throws Exception {
        Map<String, Object> body = registration("member@example.com", PASSWORD, "park");
        body.put(field, "attacker-value");
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.errorCode").value("INVALID_REQUEST"));
        verify(users, never()).insert(any(UserEntity.class));
    }

    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void registrationRejectsInvalidPasswordLengths(String password) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(registration("member@example.com", password, "park"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fieldErrors.password").exists());
        verify(users, never()).insert(any(UserEntity.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "email", "password", "parkId"})
    void requiredRegistrationFieldsAreValidated(String field) throws Exception {
        Map<String, Object> body = registration("member@example.com", PASSWORD, "park");
        body.remove(field);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fieldErrors." + field).exists());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"RANGER", "LIAISON_OFFICER", "RESEARCHER"})
    void managerCanCreateEachStaffRole(Role role) throws Exception {
        UserEntity manager = account(Role.PARK_MANAGER, false);
        MvcResult result = mvc.perform(post("/api/v1/users").header("Authorization", bearer(manager))
                .contentType(MediaType.APPLICATION_JSON).content(json(staff(role.name(), Set.of("park")))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.role").value(role.name()))
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
                .andExpect(jsonPath("$.data.email").value("staff@example.com"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist()).andReturn();
        String id = mapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
        assertThat(encoder.matches(PASSWORD, accounts.get(id).getPasswordHash())).isTrue();
        String token = login("staff@example.com", PASSWORD);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.passwordChangeRequired").value(true));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, mode = EnumSource.Mode.EXCLUDE, names = "PARK_MANAGER")
    void otherRolesCannotCreateStaff(Role role) throws Exception {
        mvc.perform(post("/api/v1/users").header("Authorization", bearer(account(role, false)))
                .contentType(MediaType.APPLICATION_JSON).content(json(staff("RANGER", Set.of("park")))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.errorCode").value("ACCESS_DENIED"));
        verify(users, never()).insert(any(UserEntity.class));
    }

    @Test
    void anonymousUsersCannotCreateStaffOrChangePasswords() throws Exception {
        mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content(json(staff("RANGER", Set.of("park"))))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PARK_MANAGER", "COMMUNITY_MEMBER", "ADMIN", ""})
    void managerCannotAssignUnsupportedStaffRoles(String role) throws Exception {
        mvc.perform(post("/api/v1/users").header("Authorization", bearer(account(Role.PARK_MANAGER, false)))
                .contentType(MediaType.APPLICATION_JSON).content(json(staff(role, Set.of("park")))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.fieldErrors.role").exists());
        verify(users, never()).insert(any(UserEntity.class));
    }

    @Test
    void managerCannotAssignAnyParkOutsideTheirScope() throws Exception {
        mvc.perform(post("/api/v1/users").header("Authorization", bearer(account(Role.PARK_MANAGER, false)))
                .contentType(MediaType.APPLICATION_JSON).content(json(staff("RANGER", Set.of("park", "other")))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.errorCode").value("PARK_ACCESS_DENIED"));
        verify(users, never()).insert(any(UserEntity.class));
    }

    @Test
    void staffAssignmentRejectsNonexistentAndEmptyParks() throws Exception {
        UserEntity manager = account(Role.PARK_MANAGER, false);
        manager.setParkIds(Set.of("missing-park"));
        mvc.perform(post("/api/v1/users").header("Authorization", bearer(manager)).contentType(MediaType.APPLICATION_JSON)
                .content(json(staff("RANGER", Set.of("missing-park"))))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/users").header("Authorization", bearer(manager)).contentType(MediaType.APPLICATION_JSON)
                .content(json(staff("RANGER", Set.of())))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.parkIds").exists());
        verify(users, never()).insert(any(UserEntity.class));
    }

    @Test
    void temporaryPasswordBlocksOperationalAccessUntilChanged() throws Exception {
        UserEntity staff = account(Role.RANGER, true);
        String token = login(staff.getNormalizedEmail(), PASSWORD);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        for (String path : List.of("/parks", "/patrol-routes", "/alerts", "/media/image/content")) {
            mvc.perform(get("/api/v1" + path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.errorCode").value("PASSWORD_CHANGE_REQUIRED"));
        }
        mvc.perform(post("/api/v1/users").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json(staff("RANGER", Set.of("park")))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.errorCode").value("PASSWORD_CHANGE_REQUIRED"));
        changePassword(token, PASSWORD, NEW_PASSWORD).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        String newToken = login(staff.getNormalizedEmail(), NEW_PASSWORD);
        mvc.perform(get("/api/v1/parks").header("Authorization", "Bearer " + newToken)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void passwordChangeWorksForEveryRoleAndInvalidatesAllPreviousTokens(Role role) throws Exception {
        UserEntity user = account(role, false);
        String token = tokens.issue(user);
        String secondToken = tokens.issue(user);
        changePassword(token, PASSWORD, NEW_PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(false))
                .andExpect(jsonPath("$.data.reloginRequired").value(true));
        UserEntity changed = accounts.get(user.getId());
        assertThat(changed.getTokenVersion()).isEqualTo(1);
        assertThat(changed.getRole()).isEqualTo(role);
        assertThat(changed.getParkIds()).containsExactly("park");
        assertThat(encoder.matches(NEW_PASSWORD, changed.getPasswordHash())).isTrue();
        for (String oldToken : List.of(token, secondToken)) {
            mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + oldToken)).andExpect(status().isUnauthorized());
            changePassword(oldToken, NEW_PASSWORD, PASSWORD).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", user.getNormalizedEmail(), "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
        String newToken = login(user.getNormalizedEmail(), NEW_PASSWORD);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + newToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordChangeRequired").value(false));
    }

    @Test
    void wrongCurrentPasswordCannotChangeAccount() throws Exception {
        UserEntity user = account(Role.RANGER, true);
        changePassword(tokens.issue(user), "incorrect-password", NEW_PASSWORD).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.errorCode").value("INVALID_CREDENTIALS"));
        verify(passwords, never()).changePassword(any(), anyString());
        assertThat(user.isPasswordChangeRequired()).isTrue();
    }

    @Test
    void passwordReuseAndInvalidNewPasswordsAreRejected() throws Exception {
        String token = tokens.issue(account(Role.COMMUNITY_MEMBER, false));
        changePassword(token, PASSWORD, PASSWORD).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.errorCode").value("VALIDATION_FAILED"));
        changePassword(token, PASSWORD, "short").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.newPassword").exists());
        changePassword(token, PASSWORD, "漢".repeat(25)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.newPassword").exists());
        verify(passwords, never()).changePassword(any(), anyString());
    }

    @Test
    void lostConcurrentPasswordChangeReturnsConflict() throws Exception {
        String token = tokens.issue(account(Role.RANGER, true));
        when(passwords.changePassword(any(), anyString())).thenReturn(null);
        changePassword(token, PASSWORD, NEW_PASSWORD).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.errorCode").value("PASSWORD_CHANGE_CONFLICT"));
    }

    @Test
    void registrationStorageFailureDoesNotExposeInternalDetails() throws Exception {
        when(users.findByNormalizedEmail("member@example.com"))
                .thenThrow(new DataAccessResourceFailureException("private connection details"));
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(registration("member@example.com", PASSWORD, "park"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.errorDescription").value("The service is temporarily unavailable."));
    }

    static Stream<String> invalidPasswords() {
        return Stream.of("short", "漢".repeat(25), "a".repeat(73), "😀".repeat(8));
    }

    private UserEntity account(Role role, boolean passwordChangeRequired) {
        UserEntity user = UserEntity.builder().id(role.name()).name("Test User")
                .normalizedEmail(role.name().toLowerCase(Locale.ROOT) + "@example.com")
                .passwordHash(encoder.encode(PASSWORD)).role(role).parkIds(Set.of("park"))
                .active(true).passwordChangeRequired(passwordChangeRequired).build();
        accounts.put(user.getId(), user);
        return user;
    }

    private Map<String, Object> registration(String email, String password, String parkId) {
        return new HashMap<>(Map.of("name", "  Villager  ", "email", email, "password", password, "parkId", parkId));
    }

    private Map<String, Object> staff(String role, Set<String> parkIds) {
        return Map.of("name", "New Staff", "email", "STAFF@example.com",
                "temporaryPassword", PASSWORD, "role", role, "parkIds", parkIds);
    }

    private String bearer(UserEntity user) {
        return "Bearer " + tokens.issue(user);
    }

    private String json(Object body) {
        return mapper.writeValueAsString(body);
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password)))).andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).at("/data/accessToken").asText();
    }

    private ResultActions changePassword(
            String token, String currentPassword, String newPassword) throws Exception {
        return mvc.perform(post("/api/v1/auth/change-password").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", currentPassword, "newPassword", newPassword))));
    }
}
