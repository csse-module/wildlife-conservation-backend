package com.wildlife.wildlife_conservationbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wildlife.media")
public class MediaStorageProperties {
    private String directory = "uploads";
}
