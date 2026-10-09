package com.wildlife.wildlife_conservationbackend.config;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wildlife.registration")
@Data
public class CommunityRegistrationProperties {
    private Set<String> communityParkIds = Set.of("park-yala");
}
