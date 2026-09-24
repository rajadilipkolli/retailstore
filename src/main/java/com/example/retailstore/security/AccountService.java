package com.example.retailstore.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService implements UserDetailsService {

    private static final Duration RESET_TOKEN_LIFETIME = Duration.ofMinutes(30);
    private static final String ADMIN_EMAIL = "admin@retailstore.com";
    private static final String BOOTSTRAP_PASSWORD_CHANGE_REQUIRED = "BOOTSTRAP_PASSWORD_CHANGE_REQUIRED";
    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    private final ConcurrentHashMap<String, ResetRequest> resetRequests = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final UserAccountRepository accountRepository;
    private final String resetUrl;
    private final Clock clock;

    /**
     * Creates the administrator with an unusable random initial password and emails a one-time
     * reset link. An existing account is left untouched, including its password and roles.
     */
    @Transactional
    public void bootstrapAdmin() {
        if (accountRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            return;
        }
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        accountRepository.save(new UserAccount(
                ADMIN_EMAIL,
                passwordEncoder.encode(password),
                Set.of("ADMIN", BOOTSTRAP_PASSWORD_CHANGE_REQUIRED),
                true));
        requestPasswordReset(ADMIN_EMAIL);
    }

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
    @Transactional
    public void onboard(String email, String rawPassword, Set<String> roles) {
        String normalizedEmail = normalize(email);
        UserAccount account = accountRepository
                .findByEmail(normalizedEmail)
                .orElseGet(() -> new UserAccount(normalizedEmail, passwordEncoder.encode(rawPassword), roles, true));
        account.update(passwordEncoder.encode(rawPassword), roles, true);
        accountRepository.save(account);
    }

    /** Creates an account only when its email is unused, leaving existing credentials unchanged. */
    @Transactional
    public void onboardIfAbsent(String email, String rawPassword, Set<String> roles) {
        String normalizedEmail = normalize(email);
        if (accountRepository.findByEmail(normalizedEmail).isEmpty()) {
            accountRepository.save(new UserAccount(normalizedEmail, passwordEncoder.encode(rawPassword), roles, true));
        }
    }

    /**
     * Registers an account that remains disabled until onboarding.
     *
     * @param email account email address
     * @param rawPassword unencoded account password
     * @param roles requested account roles
     * @return {@code true} when the account was created, or {@code false} when the email already exists
     */
    @Transactional
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
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserAccount account = accountRepository.findByEmail(normalize(email)).orElse(null);
        if (account == null
                || !account.isOnboarded()
                || account.getRoles().contains(BOOTSTRAP_PASSWORD_CHANGE_REQUIRED)) {
            throw new UsernameNotFoundException("Account not found");
        }
        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .roles(account.getRoles().toArray(String[]::new))
                .build();
    }

    /**
     * Sends a single-use password-reset link for an onboarded account.
     * If email delivery fails, the token is discarded and the same neutral response is returned.
     *
     * @param email account email address
     * @return a neutral response that does not reveal whether the account exists
     * @throws RuntimeException if the account lookup fails
     */
    @Transactional
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
                LOG.warn("Password reset email delivery failed: {}", exception.getMessage());
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
    @Transactional
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
        Set<String> roles = new HashSet<>(account.getRoles());
        roles.remove(BOOTSTRAP_PASSWORD_CHANGE_REQUIRED);
        account.update(passwordEncoder.encode(newPassword), roles, account.isOnboarded());
        accountRepository.save(account);
        return true;
    }

    /**
     * Checks whether a reset token can currently be used.
     *
     * @param token single-use reset token
     * @return {@code true} when the token is present, unexpired, and belongs to an onboarded account
     */
    @Transactional(readOnly = true)
    public boolean isResetTokenUsable(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        ResetRequest request = resetRequests.get(token);
        if (request == null || request.expiresAt().isBefore(Instant.now(clock))) {
            return false;
        }
        return accountRepository
                .findByEmail(request.email())
                .map(UserAccount::isOnboarded)
                .orElse(false);
    }

    /**
     * Finds a pending reset token for an account.
     *
     * @param email account email address
     * @return a pending token, when one exists
     */
    @Transactional(readOnly = true)
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
