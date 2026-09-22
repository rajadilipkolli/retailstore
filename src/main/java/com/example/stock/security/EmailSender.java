package com.example.stock.security;

public interface EmailSender {

    void sendPasswordReset(String recipient, String resetUrl);
}