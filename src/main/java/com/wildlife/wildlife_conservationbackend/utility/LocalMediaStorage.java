package com.wildlife.wildlife_conservationbackend.utility;

import com.wildlife.wildlife_conservationbackend.config.MediaStorageProperties;
import com.wildlife.wildlife_conservationbackend.exception.ApiException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocalMediaStorage implements MediaStorage {
    private final MediaStorageProperties properties;

    @Override
    public String store(String checksum, byte[] content, String contentType) {
        String key = checksum + ("image/png".equals(contentType) ? ".png" : ".jpg");
        try {
            Path root = Path.of(properties.getDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(root);
            Path target = resolve(root.toRealPath(), key);
            if (Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                return key;
            }
            Path temporary = Files.createTempFile(target.getParent(), "upload-", ".tmp");
            try {
                Files.write(temporary, content);
                publish(temporary, target);
            } finally {
                Files.deleteIfExists(temporary);
            }
            return key;
        } catch (IOException exception) {
            log.error("Image storage write failed", exception);
            throw unavailable();
        }
    }

    @Override
    public byte[] read(String storageKey) {
        try {
            Path root = Path.of(properties.getDirectory()).toAbsolutePath().normalize().toRealPath();
            Path target = resolve(root, storageKey);
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) || Files.size(target) > ImageValidator.MAX_BYTES) {
                throw unavailable();
            }
            return Files.readAllBytes(target);
        } catch (IOException exception) {
            log.error("Image storage read failed", exception);
            throw unavailable();
        }
    }

    private Path resolve(Path root, String key) {
        if (key == null || !key.matches("[a-f0-9]{64}\\.(png|jpg)")) {
            throw unavailable();
        }
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw unavailable();
        }
        return target;
    }

    private void publish(Path temporary, Path target) throws IOException {
        try {
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
        } catch (FileAlreadyExistsException exception) {
            log.debug("Concurrent image upload already stored");
        }
    }

    private ApiException unavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE", "Image storage is temporarily unavailable.");
    }
}
