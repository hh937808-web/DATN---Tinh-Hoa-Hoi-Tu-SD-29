package com.example.datn_sd_29.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class PasswordTestController {

    private final PasswordEncoder passwordEncoder;

    @GetMapping("/password-hash")
    public Map<String, Object> testPasswordHash(@RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        
        String encoded = passwordEncoder.encode(password);
        result.put("rawPassword", password);
        result.put("encodedPassword", encoded);
        result.put("matches", passwordEncoder.matches(password, encoded));
        
        // Test với hash cố định
        String fixedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        result.put("matchesFixedHash", passwordEncoder.matches(password, fixedHash));
        result.put("fixedHash", fixedHash);
        
        log.info("Password test: raw={}, encoded={}, matches={}, matchesFixed={}", 
                password, encoded, result.get("matches"), result.get("matchesFixedHash"));
        
        return result;
    }
    
    @GetMapping("/password-verify")
    public Map<String, Object> verifyPassword(
            @RequestParam String password,
            @RequestParam String hash) {
        Map<String, Object> result = new HashMap<>();
        
        boolean matches = passwordEncoder.matches(password, hash);
        result.put("password", password);
        result.put("hash", hash);
        result.put("matches", matches);
        
        log.info("Password verify: password={}, hash={}, matches={}", password, hash, matches);
        
        return result;
    }
}
