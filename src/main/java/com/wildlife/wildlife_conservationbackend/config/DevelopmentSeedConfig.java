package com.wildlife.wildlife_conservationbackend.config;

import com.wildlife.wildlife_conservationbackend.domain.GeoPoint;
import com.wildlife.wildlife_conservationbackend.domain.Location;
import com.wildlife.wildlife_conservationbackend.entity.CameraTrapEntity;
import com.wildlife.wildlife_conservationbackend.enums.LocationSource;
import com.wildlife.wildlife_conservationbackend.repository.CameraTrapRepository;
import com.wildlife.wildlife_conservationbackend.entity.ParkArea;
import com.wildlife.wildlife_conservationbackend.entity.ParkEntity;
import com.wildlife.wildlife_conservationbackend.entity.PatrolRouteEntity;
import com.wildlife.wildlife_conservationbackend.entity.UserEntity;
import com.wildlife.wildlife_conservationbackend.enums.Role;
import com.wildlife.wildlife_conservationbackend.repository.ParkRepository;
import com.wildlife.wildlife_conservationbackend.repository.PatrolRouteRepository;
import com.wildlife.wildlife_conservationbackend.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
@ConditionalOnProperty(name = "wildlife.seed.enabled", havingValue = "true")
@Slf4j
public class DevelopmentSeedConfig {
    @Bean
    public ApplicationRunner developmentSeed(UserRepository users, ParkRepository parks, PatrolRouteRepository routes,
                                             CameraTrapRepository cameraTraps,
                                             PasswordEncoder encoder, @Value("${wildlife.seed.password:}") String password) {
        return arguments -> {
            if (password.codePointCount(0, password.length()) < 6 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("DEV_SEED_PASSWORD must contain at least 6 characters and at most 72 UTF-8 bytes.");
            }
            String parkId = "park-yala";
            if (!parks.existsById(parkId)) {
                parks.insert(new ParkEntity(parkId, "Yala National Park", "Asia/Colombo",
                        List.of(new ParkArea("area-b1", "Block 1"))));
            }
            if (!routes.existsById("route-demo")) {
                routes.insert(new PatrolRouteEntity("route-demo", parkId, "area-b1", "Demonstration route", 1000,
                        List.of(new GeoPoint(6.37, 81.50), new GeoPoint(6.38, 81.50))));
            }
            seedUser(users, encoder, password, "manager", Role.PARK_MANAGER);
            if (!cameraTraps.existsById("CT-DEMO")) {
                cameraTraps.insert(new CameraTrapEntity("CT-DEMO", parkId, "area-b1", "Demonstration camera trap",
                        new Location(6.37, 81.50, LocationSource.GPS, 8.0)));
            }
            seedUser(users, encoder, password, "ranger", Role.RANGER);
            seedUser(users, encoder, password, "liaison", Role.LIAISON_OFFICER);
            seedUser(users, encoder, password, "researcher", Role.RESEARCHER);
            seedUser(users, encoder, password, "community", Role.COMMUNITY_MEMBER);
            log.info("Development seed ready; existing records were preserved");
        };
    }

    private void seedUser(UserRepository users, PasswordEncoder encoder, String password, String name, Role role) {
        String email = name + "@wildguard.local";
        if (users.findByNormalizedEmail(email).isEmpty()) {
            users.insert(new UserEntity("usr-" + name, "Demo " + name, email, encoder.encode(password),
                    role, Set.of("park-yala"), true));
        }
    }
}
