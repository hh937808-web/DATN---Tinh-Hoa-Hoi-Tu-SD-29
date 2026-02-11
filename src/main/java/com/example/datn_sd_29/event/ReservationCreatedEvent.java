package com.example.datn_sd_29.event;

import java.time.Instant;

public class ReservationCreatedEvent {
    private final String email;
    private final String reservationCode;
    private final Instant reservedAt;

    public ReservationCreatedEvent(String email, String reservationCode, Instant reservedAt) {
        this.email = email;
        this.reservationCode = reservationCode;
        this.reservedAt = reservedAt;
    }

    public String getEmail() {
        return email;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }
}
