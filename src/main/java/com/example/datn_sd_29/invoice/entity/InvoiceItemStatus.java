package com.example.datn_sd_29.invoice.entity;

public enum InvoiceItemStatus {
    PENDING,      // Dessert ordered but held — waiting for staff to activate
    ORDERED, IN_PROGRESS, DONE, SERVED, CANCELLED
}
