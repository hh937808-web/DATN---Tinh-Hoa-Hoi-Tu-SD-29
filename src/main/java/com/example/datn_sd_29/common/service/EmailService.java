package com.example.datn_sd_29.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    public void sendOtpEmail(String to, String otpCode, int expiresInMinutes) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setSubject("Mã OTP xác thực tài khoản");
        mail.setText(
                "Mã OTP của bạn là: " + otpCode + "\n" +
                        "Mã hiệu lực trong " + expiresInMinutes + " phút. \n" +
                        "Lưu ý: đừng chia sẻ mã OTP."
        );
        mailSender.send(mail);
    }

    public void sendReservationDetailsEmail(
            String to,
            String reservationCode,
            LocalDateTime reservedAt,
            Integer guestCount,
            String promotionType,
            String note,
            List<String> tableCodes
    ) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(mailDateTimeFormat);
        String timeText = reservedAt != null ? reservedAt.format(formatter) : "";
        String tablesText = (tableCodes == null || tableCodes.isEmpty())
                ? "N/A"
                : String.join(", ", tableCodes);

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setTo(to);
        mail.setSubject("Thông tin đặt bàn");
        mail.setText(
                "Mã bàn đặt: " + reservationCode + "\n" +
                        "Thời gian đến: " + timeText + "\n" +
                        "Số khách: " + guestCount + "\n" +
                        "Ưu đãi: " + (promotionType == null ? "" : promotionType) + "\n" +
                        "Ghi chú: " + (note == null ? "" : note) + "\n" +
                        "Bàn: " + tablesText
        );
        mailSender.send(mail);
    }
}
