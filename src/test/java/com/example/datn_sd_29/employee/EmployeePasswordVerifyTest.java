package com.example.datn_sd_29.employee;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
public class EmployeePasswordVerifyTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testPasswordMatch() {
        // Password hash từ DB của staff2
        String hashFromDB = "$2a$10$rZgq1qxGKx.l5y..."; // Thay bằng hash thật từ DB
        
        // Test với password "123"
        boolean matches = passwordEncoder.matches("123", hashFromDB);
        
        System.out.println("=".repeat(50));
        System.out.println("Password Hash from DB: " + hashFromDB);
        System.out.println("Test Password: 123");
        System.out.println("Matches: " + matches);
        System.out.println("=".repeat(50));
        
        // Tạo hash mới để so sánh
        String newHash = passwordEncoder.encode("123");
        System.out.println("New Hash for '123': " + newHash);
        System.out.println("New Hash Matches: " + passwordEncoder.matches("123", newHash));
    }
}
