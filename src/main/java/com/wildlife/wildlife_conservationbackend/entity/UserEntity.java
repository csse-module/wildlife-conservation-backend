package com.wildlife.wildlife_conservationbackend.entity;

import com.wildlife.wildlife_conservationbackend.enums.Role;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Document("users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class UserEntity {
    @MongoId
    @ToString.Include
    private String id;
    private String name;
    @Indexed(unique = true)
    private String normalizedEmail;
    private String passwordHash;
    @ToString.Include
    private Role role;
    private Set<String> parkIds;
    private boolean active;
    private boolean passwordChangeRequired;
    private long tokenVersion;

    public UserEntity(String id, String name, String normalizedEmail, String passwordHash,
                      Role role, Set<String> parkIds, boolean active) {
        this(id, name, normalizedEmail, passwordHash, role, parkIds, active, false, 0);
    }
}
