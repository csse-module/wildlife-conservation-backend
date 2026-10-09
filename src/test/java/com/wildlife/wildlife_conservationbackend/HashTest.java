package com.wildlife.wildlife_conservationbackend;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashTest {
    @Test
    public void generateHash() {
        System.out.println("HASH_START:" + new BCryptPasswordEncoder().encode("admin123") + ":HASH_END");
    }
}
