package com.example.stock.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AccountService implements UserDetailsService {

    private static final Duration RESET_TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final Map<String, ResetRequest> resetRequests = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final String resetUrl;

    public AccountService(PasswordEncoder passwordEncoder, EmailSender emailSender,
            @org.springframework.beans.factory.annotation.Value("${app.mail.reset-url:http://localhost:8080/reset-password}") String resetUrl) {
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.resetUrl = resetUrl;
        onboard("manager@example.com", "password", Set.of("INVENTORY_MANAGER"));
        onboard("warehouse@example.com", "password", Set.of("WAREHOUSE_STAFF"));
        onboard("purchasing@example.com", "password", Set.of("PURCHASING_MANAGER"));
    }

    public void onboard(String email, String rawPassword, Set<String> roles) {
        String normalizedEmail = normalize(email);
        accounts.put(normalizedEmail,
                new Account(normalizedEmail, passwordEncoder.encode(rawPassword), roles, true));
    }

    public void register(String email, String rawPassword, Set<String> roles) {
        String normalizedEmail = normalize(email);
        accounts.put(normalizedEmail,
                new Account(normalizedEmail, passwordEncoder.encode(rawPassword), roles, false));
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accounts.get(normalize(email));
        if (account == null || !account.onboarded()) {
            throw new UsernameNotFoundException("Account not found");
        }
        return User.withUsername(account.email())
                .password(account.passwordHash())
                .roles(account.roles().toArray(String[]::new))
                .build();
    }

    public String requestPasswordReset(String email) {
        String normalizedEmail = normalize(email);
        if (accounts.containsKey(normalizedEmail)) {
            String token = UUID.randomUUID().toString();
            resetRequests.put(token, new ResetRequest(normalizedEmail, Instant.now().plus(RESET_TOKEN_LIFETIME)));
            try {
                emailSender.sendPasswordReset(normalizedEmail, resetUrl + "?token=" + token);
            } catch (RuntimeException exception) {
                resetRequests.remove(token);
                logger.warn("Password reset email delivery failed: {}", exception.getMessage());
            }
        }
        return "If an account exists for that email, a reset link has been sent.";
    }

    public boolean resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            return false;
        }
        ResetRequest request = resetRequests.remove(token);
        if (request == null || request.expiresAt().isBefore(Instant.now())) {
            return false;
        }
        Account account = accounts.get(request.email());
        if (account == null || !account.onboarded()) {
            return false;
        }
        accounts.put(request.email(), new Account(account.email(), passwordEncoder.encode(newPassword),
                account.roles(), account.onboarded()));
        return true;
    }

    public Optional<String> latestResetTokenFor(String email) {
        String normalizedEmail = normalize(email);
        return resetRequests.entrySet().stream()
                .filter(entry -> entry.getValue().email().equals(normalizedEmail))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record ResetRequest(String email, Instant expiresAt) {
    }
}