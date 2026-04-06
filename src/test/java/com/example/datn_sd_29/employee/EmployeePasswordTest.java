package com.example.datn_sd_29.employee;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class EmployeePasswordTest {

    @Test
    public void testPasswordEncoding() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String rawPassword = "123";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Encoded password: " + encodedPassword);
        System.out.println("Password matches: " + encoder.matches(rawPassword, encodedPassword));
        
        // Test với password đã có trong SQL script
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        System.out.println("\nTesting existing hash from SQL:");
        System.out.println("Existing hash: " + existingHash);
        System.out.println("Password '123' matches existing hash: " + encoder.matches("123", existingHash));
    }
}
