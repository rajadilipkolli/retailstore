package com.example.stock.security;

public interface EmailSender {

    /**
     * Sends a password-reset link to an account holder.
     *
     * @param recipient destination email address
     * @param resetUrl single-use password-reset URL
     */
    void sendPasswordReset(String recipient, String resetUrl);
}
