package com.example.datn_sd_29.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendReservationEmail(String to, String code, Instant time) {

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setSubject("Xác nhận đặt bàn");
        mail.setText(
                "Bạn đã đặt bàn thành công.\n" +
                        "Mã đặt bàn: " + code + "\n" +
                        "Thời gian: " + time
        );

        mailSender.send(mail);
    }
}
