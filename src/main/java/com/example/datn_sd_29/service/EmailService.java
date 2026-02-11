package com.example.datn_sd_29.service;

import com.example.datn_sd_29.dto.ReservationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${app.mail.timezone:Asia/Ho_Chi_Minh}")
    private String mailTimeZone;

    @Value("${app.mail.datetime-format:dd/MM/yyyy HH:mm}")
    private String mailDateTimeFormat;
    public void sendReservationEmail(String to, String code, Instant time) {

        ZoneId zoneId = ZoneId.of(mailTimeZone);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mailDateTimeFormat);
        String timeText = time.atZone(zoneId).format(formatter);

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setSubject("Xác nhận đặt bàn");
        mail.setText(
                "Bạn đã đặt bàn thành công.\n" +
                        "Mã đặt bàn: " + code + "\n" +
                        "Thời gian: " + timeText
        );

        mailSender.send(mail);
    }
}
