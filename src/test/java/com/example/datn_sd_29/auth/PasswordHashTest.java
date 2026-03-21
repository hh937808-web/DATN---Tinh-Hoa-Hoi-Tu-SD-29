package com.example.datn_sd_29.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class PasswordHashTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testPasswordHash() {
        String rawPassword = "123";
        String storedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhCu";
        
        boolean matches = passwordEncoder.matches(rawPassword, storedHash);
        
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Stored hash: " + storedHash);
        System.out.println("Matches: " + matches);
        
        assertTrue(matches, "Password should match the hash");
    }

    @Test
    public void generateNewHash() {
        String rawPassword = "123";
        String newHash = passwordEncoder.encode(rawPassword);
        
        System.out.println("New hash for '123': " + newHash);
        System.out.println("Verification: " + passwordEncoder.matches(rawPassword, newHash));
    }
}
