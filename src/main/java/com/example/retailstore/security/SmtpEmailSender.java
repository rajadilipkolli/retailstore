package com.example.retailstore.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String from;

    /**
     * Creates an SMTP-backed password-reset sender.
     *
     * @param mailSender configured SMTP client
     * @param from sender address for reset messages
     */
    public SmtpEmailSender(
            JavaMailSender mailSender, @Value("${app.mail.from:no-reply@retailstore.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /**
     * Sends a password-reset message through the configured SMTP server.
     *
     * @param recipient destination email address
     * @param resetUrl single-use password-reset URL
     */
    @Override
    public void sendPasswordReset(String recipient, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Reset your Retail Store password");
        message.setText("Use this single-use link to reset your password: " + resetUrl);
        mailSender.send(message);
    }
}
