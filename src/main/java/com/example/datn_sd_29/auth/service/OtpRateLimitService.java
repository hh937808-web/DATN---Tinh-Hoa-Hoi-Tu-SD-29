package com.example.datn_sd_29.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpRateLimitService {

    private static final String PURPOSE = "REGISTER";
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration SEND_WINDOW = Duration.ofMinutes(15);
    private static final int SEND_MAX = 5;
    private static final Duration FAIL_WINDOW = Duration.ofHours(24);

    private final StringRedisTemplate redis;

    private String keyCooldown(String email) { return "otp:cooldown:" + PURPOSE + ":" + email; }
    private String keySendCount(String email) { return "otp:sendcount:"+ PURPOSE + ":" + email; }
    private String keyFailCount(String email) { return "otp:failcount:" + PURPOSE + ":" + email; }
    private String keyLock(String email) { return "otp:lock:" + PURPOSE + ":" + email; }

    public void assertNotLocked(String email) {
        Long ttl = redis.getExpire(keyLock(email), TimeUnit.SECONDS);
        if (ttl != null && ttl > 0) {
            throw new IllegalArgumentException("Tài khoản OTP đang bị khóa, thử lại sau " + ttl + " giây");
        }
    }

    public void assertCanSendOtp(String email) {
        Long cooldown = redis.getExpire(keyCooldown(email), TimeUnit.SECONDS);
        if (cooldown != null && cooldown > 0) {
            throw new IllegalArgumentException("Vui lòng đợi " + cooldown + " giây để gửi lại OTP");
        }

        String val = redis.opsForValue().get(keySendCount(email));
        long send = (val == null) ? 0 : Long.parseLong(val);
        if (send >= SEND_MAX) {
            Long ttl = redis.getExpire(keySendCount(email), TimeUnit.SECONDS);
            long wait = (ttl == null || ttl < 0) ? SEND_WINDOW.getSeconds() : ttl;
            throw new IllegalArgumentException("Bạn đã gửi OTP quá nhiều lần, thử lại sau " + wait + " giây");
        }
    }

    public void markOtpSend(String email) {
        Long count = redis.opsForValue().increment(keySendCount(email));
        if (count != null && count == 1L) {
            redis.expire(keySendCount(email), SEND_WINDOW);
        }
        redis.opsForValue().set(keyCooldown(email), "1", SEND_COOLDOWN);
    }

    public void onVerifyFailed(String email) {
        Long count = redis.opsForValue().increment(keyFailCount(email));
        if (count != null && count == 1L) {
            redis.expire(keyFailCount(email), FAIL_WINDOW);
        }
        if (count == null) return;

        if (count >= 15) {
            redis.opsForValue().set(keyLock(email), "1", Duration.ofHours(1));
        } else if (count >= 10) {
            redis.opsForValue().set(keyLock(email), "1", Duration.ofMinutes(30));
        } else if (count >= 5) {
            redis.opsForValue().set(keyLock(email), "1", Duration.ofMinutes(10));
        }
    }

    public void onVerifySuccess(String email) {
        redis.delete(List.of(keyFailCount(email), keyLock(email)));
    }
}
