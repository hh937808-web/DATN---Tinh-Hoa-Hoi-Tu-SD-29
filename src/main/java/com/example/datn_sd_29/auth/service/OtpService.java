package com.example.datn_sd_29.auth.service;

import com.example.datn_sd_29.auth.dto.RegisterRequest;
import com.example.datn_sd_29.auth.dto.RegisterResponse;
import com.example.datn_sd_29.auth.dto.SendOtpRequest;
import com.example.datn_sd_29.auth.dto.SendOtpResponse;
import com.example.datn_sd_29.auth.dto.VerifyOtpRequest;
import com.example.datn_sd_29.auth.entity.OtpChallenge;
import com.example.datn_sd_29.auth.entity.PendingRegister;
import com.example.datn_sd_29.auth.repository.OtpChallengeRepository;
import com.example.datn_sd_29.auth.repository.PendingRegisterRepository;
import com.example.datn_sd_29.common.service.EmailService;
import com.example.datn_sd_29.customer.entity.Customer;
import com.example.datn_sd_29.customer.repository.CustomerRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String PURPOSE_REGISTER = "REGISTER";
    private static final int OTP_TTL_MINUTES = 5;
    private static final int OTP_MAX_ATTEMPTS = 3;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;
    private static final int PENDING_TTL_MINUTES = 30;

    private final OtpChallengeRepository otpChallengeRepository;
    private final PendingRegisterRepository pendingRegisterRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public SendOtpResponse startRegister(RegisterRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = normalizedEmail(request.getEmail());
        if (customerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Instant now = Instant.now();
        PendingRegister pending = getActivePendingOrNull(normalizedEmail, now);

        OtpChallenge challenge = issueRegisterOtp(normalizedEmail, httpRequest, now);

        if (pending == null) {
            pending = new PendingRegister();
            pending.setEmail(normalizedEmail);
            pending.setCreatedAt(now);
        }

        pending.setOtpChallengeId(challenge.getId());
        pending.setFullName(trimToNull(request.getFullName()));
        pending.setPhoneNumber(trimToNull(request.getPhoneNumber()));
        pending.setDateOfBirth(request.getDateOfBirth());
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setOtpVerifiedAt(null);
        pending.setExpiresAt(now.plus(PENDING_TTL_MINUTES, ChronoUnit.MINUTES));
        pending.setCompletedAt(null);
        pending.setInvalidatedAt(null);
        pending.setRequestIp(extractClientIp(httpRequest));
        pending.setUserAgent(extractUserAgent(httpRequest));
        pending.setUpdatedAt(now);

        pendingRegisterRepository.save(pending);

        return new SendOtpResponse(
                normalizedEmail,
                challenge.getExpiresAt(),
                OTP_RESEND_COOLDOWN_SECONDS
        );
    }

    @Transactional
    public SendOtpResponse sendRegisterOtp(SendOtpRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = normalizedEmail(request.getEmail());
        if (customerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Instant now = Instant.now();
        PendingRegister pending = getActivePendingOrNull(normalizedEmail, now);
        if (pending == null) {
            throw new IllegalArgumentException("No pending register. Please submit register info first.");
        }

        OtpChallenge challenge = issueRegisterOtp(normalizedEmail, httpRequest, now);

        pending.setOtpChallengeId(challenge.getId());
        pending.setRequestIp(extractClientIp(httpRequest));
        pending.setUserAgent(extractUserAgent(httpRequest));
        pending.setUpdatedAt(now);
        pendingRegisterRepository.save(pending);

        return new SendOtpResponse(
                normalizedEmail,
                challenge.getExpiresAt(),
                OTP_RESEND_COOLDOWN_SECONDS
        );
    }

    @Transactional
    public RegisterResponse verifyRegisterOtp(VerifyOtpRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = normalizedEmail(request.getEmail());
        Instant now = Instant.now();

        OtpChallenge challenge = otpChallengeRepository
                .findLatestActive(normalizedEmail, PURPOSE_REGISTER)
                .orElseThrow(() -> new IllegalArgumentException("OTP not found. Please request OTP first."));

        if (challenge.getExpiresAt() != null && now.isAfter(challenge.getExpiresAt())) {
            challenge.setInvalidatedAt(now);
            challenge.setUpdatedAt(now);
            otpChallengeRepository.save(challenge);
            throw new IllegalArgumentException("OTP expired. Please request a new OTP.");
        }

        int attemptCount = safeInt(challenge.getAttemptCount());
        int maxAttempts = safeInt(challenge.getMaxAttempts());
        if (maxAttempts <= 0) {
            maxAttempts = OTP_MAX_ATTEMPTS;
        }

        if (attemptCount >= maxAttempts) {
            throw new IllegalArgumentException("OTP attempt limit exceeded.");
        }

        boolean matched = passwordEncoder.matches(request.getOtpCode(), challenge.getOtpHash());
        if (!matched) {
            attemptCount += 1;
            challenge.setAttemptCount(attemptCount);
            challenge.setUpdatedAt(now);
            if (attemptCount >= maxAttempts) {
                challenge.setInvalidatedAt(now);
            }
            otpChallengeRepository.save(challenge);
            throw new IllegalArgumentException("Invalid OTP code.");
        }

        PendingRegister pending = getActivePendingOrNull(normalizedEmail, now);
        if (pending == null) {
            throw new IllegalArgumentException("No pending register. Please register first.");
        }

        if (customerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            pending.setInvalidatedAt(now);
            pending.setUpdatedAt(now);
            pendingRegisterRepository.save(pending);
            throw new IllegalArgumentException("Email already exists");
        }

        String passwordHash = trimToNull(pending.getPasswordHash());
        if (passwordHash == null) {
            throw new IllegalArgumentException("Pending register is invalid. Please register again.");
        }

        Customer customer = new Customer();
        customer.setFullName(trimToNull(pending.getFullName()));
        customer.setEmail(normalizedEmail);
        customer.setPassword(passwordHash);
        customer.setPhoneNumber(trimToNull(pending.getPhoneNumber()));
        customer.setDateOfBirth(pending.getDateOfBirth());
        customer.setLoyaltyPoints(0);
        customer.setIsActive(true);
        customer.setCreatedAt(now);
        Customer saved = customerRepository.save(customer);

        challenge.setVerifiedAt(now);
        challenge.setConsumedAt(now);
        challenge.setUpdatedAt(now);
        otpChallengeRepository.save(challenge);

        pending.setOtpVerifiedAt(now);
        pending.setCompletedAt(now);
        pending.setRequestIp(extractClientIp(httpRequest));
        pending.setUserAgent(extractUserAgent(httpRequest));
        pending.setUpdatedAt(now);
        pendingRegisterRepository.save(pending);

        return new RegisterResponse(saved.getId(), saved.getEmail());
    }

    private PendingRegister getActivePendingOrNull(String normalizedEmail, Instant now) {
        PendingRegister pending = pendingRegisterRepository
                .findLatestActive(normalizedEmail)
                .orElse(null);

        if (pending == null) {
            return null;
        }

        if (pending.getExpiresAt() != null && now.isAfter(pending.getExpiresAt())) {
            pending.setInvalidatedAt(now);
            pending.setUpdatedAt(now);
            pendingRegisterRepository.save(pending);
            return null;
        }

        return pending;
    }

    private OtpChallenge issueRegisterOtp(String normalizedEmail, HttpServletRequest httpRequest, Instant now) {
        OtpChallenge existing = otpChallengeRepository
                .findLatestActive(normalizedEmail, PURPOSE_REGISTER)
                .orElse(null);

        if (existing != null) {
            Instant nextResendAt = existing.getNextResendAt();
            if (nextResendAt != null && now.isBefore(nextResendAt)) {
                long waitSeconds = Math.max(Duration.between(now, nextResendAt).getSeconds(), 1);
                throw new IllegalArgumentException(
                        "Please wait " + waitSeconds + " seconds before requesting a new OTP"
                );
            }

            existing.setInvalidatedAt(now);
            existing.setUpdatedAt(now);
            otpChallengeRepository.save(existing);
        }

        String otpCode = generateOtpCode();

        OtpChallenge challenge = new OtpChallenge();
        challenge.setEmail(normalizedEmail);
        challenge.setPurpose(PURPOSE_REGISTER);
        challenge.setOtpHash(passwordEncoder.encode(otpCode));
        challenge.setExpiresAt(now.plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
        challenge.setAttemptCount(0);
        challenge.setMaxAttempts(OTP_MAX_ATTEMPTS);
        challenge.setResendCount(0);
        challenge.setNextResendAt(now.plus(OTP_RESEND_COOLDOWN_SECONDS, ChronoUnit.SECONDS));
        challenge.setRequestIp(extractClientIp(httpRequest));
        challenge.setUserAgent(extractUserAgent(httpRequest));
        challenge.setCreatedAt(now);
        challenge.setUpdatedAt(now);

        OtpChallenge saved = otpChallengeRepository.save(challenge);
        emailService.sendOtpEmail(normalizedEmail, otpCode, OTP_TTL_MINUTES);

        return saved;
    }

    private String generateOtpCode() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(otp);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizedEmail(String email) {
        String normalizedEmail = trimToNull(email);
        if (normalizedEmail == null) {
            throw new IllegalArgumentException("Email is required");
        }
        return normalizedEmail.toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        return trimToNull(request.getHeader("User-Agent"));
    }
}
