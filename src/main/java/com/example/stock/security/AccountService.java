package com.example.stock.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService implements UserDetailsService {

    private static final Duration RESET_TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final ConcurrentHashMap<String, ResetRequest> resetRequests = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final UserAccountRepository accountRepository;
    private final String resetUrl;
    private final Clock clock;

    /**
     * Creates the account service backed by the persistent account store.
     *
     * @param passwordEncoder encoder used to store account passwords
     * @param emailSender sender used to deliver password-reset links
     * @param accountRepository persistent account store
     * @param resetUrl base URL for password-reset links
     * @param clock time source used to expire reset requests
     */
    public AccountService(
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            UserAccountRepository accountRepository,
            @org.springframework.beans.factory.annotation.Value(
                            "${app.mail.reset-url:http://localhost:8080/reset-password}")
                    String resetUrl,
            Clock clock) {
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.accountRepository = accountRepository;
        this.resetUrl = resetUrl;
        this.clock = clock;
    }

    /**
     * Creates or approves an account with the supplied credentials and roles.
     *
     * @param email account email address
     * @param rawPassword unencoded account password
     * @param roles roles granted to the account
     */
    public void onboard(String email, String rawPassword, Set<String> roles) {
        String normalizedEmail = normalize(email);
        UserAccount account = accountRepository
                .findByEmail(normalizedEmail)
                .orElseGet(() -> new UserAccount(normalizedEmail, passwordEncoder.encode(rawPassword), roles, true));
        account.update(passwordEncoder.encode(rawPassword), roles, true);
        accountRepository.save(account);
    }

    /**
     * Registers an account that remains disabled until onboarding.
     *
     * @param email account email address
     * @param rawPassword unencoded account password
     * @param roles requested account roles
     * @return {@code true} when the account was created, or {@code false} when the email already exists
     */
    public boolean register(String email, String rawPassword, Set<String> roles) {
        String normalizedEmail = normalize(email);
        if (accountRepository.findByEmail(normalizedEmail).isPresent()) {
            return false;
        }
        accountRepository.save(new UserAccount(normalizedEmail, passwordEncoder.encode(rawPassword), roles, false));
        return true;
    }

    /**
     * Loads an onboarded account for Spring Security authentication.
     *
     * @param email account email address
     * @return security details for the onboarded account
     * @throws UsernameNotFoundException when the account is missing or has not been onboarded
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = accountRepository.findByEmail(normalize(email)).orElse(null);
        if (account == null || !account.isOnboarded()) {
            throw new UsernameNotFoundException("Account not found");
        }
        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRoles().toArray(String[]::new))
                .build();
    }

    /**
     * Sends a single-use password-reset link for an onboarded account.
     *
     * @param email account email address
     * @return a neutral response that does not reveal whether the account exists
     */
    public String requestPasswordReset(String email) {
        String normalizedEmail = normalize(email);
        if (accountRepository
                .findByEmail(normalizedEmail)
                .filter(UserAccount::isOnboarded)
                .isPresent()) {
            String token = UUID.randomUUID().toString();
            resetRequests.put(
                    token, new ResetRequest(normalizedEmail, Instant.now(clock).plus(RESET_TOKEN_LIFETIME)));
            try {
                emailSender.sendPasswordReset(normalizedEmail, resetUrl + "?token=" + token);
            } catch (RuntimeException exception) {
                resetRequests.remove(token);
                logger.warn("Password reset email delivery failed: {}", exception.getMessage());
            }
        }
        return "If an account exists for that email, a reset link has been sent.";
    }

    /**
     * Replaces an account password when the reset token is valid and unexpired.
     *
     * @param token single-use reset token
     * @param newPassword replacement password
     * @return {@code true} when the password was changed
     */
    public boolean resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            return false;
        }
        ResetRequest request = resetRequests.remove(token);
        if (request == null || request.expiresAt().isBefore(Instant.now(clock))) {
            return false;
        }
        UserAccount account = accountRepository.findByEmail(request.email()).orElse(null);
        if (account == null || !account.isOnboarded()) {
            return false;
        }
        account.update(passwordEncoder.encode(newPassword), account.getRoles(), account.isOnboarded());
        accountRepository.save(account);
        return true;
    }

    /**
     * Finds a pending reset token for an account.
     *
     * @param email account email address
     * @return a pending token, when one exists
     */
    public Optional<String> latestResetTokenFor(String email) {
        String normalizedEmail = normalize(email);
        return resetRequests.entrySet().stream()
                .filter(entry -> entry.getValue().email().equals(normalizedEmail))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * Normalizes an email address for identity comparisons.
     *
     * @param email email address to normalize
     * @return the trimmed, lowercase address, or an empty string for {@code null}
     */
    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record ResetRequest(String email, Instant expiresAt) {}
}
