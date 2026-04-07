package com.example.datn_sd_29.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
@ConditionalOnProperty(name = "security.api.enabled", havingValue = "true", matchIfMissing = true)
public class MethodSecurityConfig {
    // This configuration will only be active when security.api.enabled=true
    // When security.api.enabled=false, @PreAuthorize annotations will be ignored
}
