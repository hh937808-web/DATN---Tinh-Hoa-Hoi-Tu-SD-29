package com.example.datn_sd_29.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class TestPasswordMatching {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testPasswordFromDB() {
        // Password hash từ DB của staff2 (sau khi update)
        String hashFromDB = "$2a$10$N9qo8buL0ickgx0ZMRZoMyeljZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        // Test với các password khác nhau
        String[] testPasswords = {"123", "1234", "password", "staff2"};
        
        System.out.println("=".repeat(70));
        System.out.println("Testing Password Matching");
        System.out.println("=".repeat(70));
        System.out.println("Hash from DB: " + hashFromDB);
        System.out.println("Hash length: " + hashFromDB.length());
        System.out.println("-".repeat(70));
        
        for (String pwd : testPasswords) {
            boolean matches = passwordEncoder.matches(pwd, hashFromDB);
            System.out.printf("Password: %-10s | Matches: %s%n", pwd, matches ? "✅ YES" : "❌ NO");
        }
        
        System.out.println("=".repeat(70));
        
        // Tạo hash mới để so sánh
        String newHash = passwordEncoder.encode("123");
        System.out.println("\nNew hash for '123': " + newHash);
        System.out.println("New hash length: " + newHash.length());
        System.out.println("New hash matches '123': " + passwordEncoder.matches("123", newHash));
    }
}
