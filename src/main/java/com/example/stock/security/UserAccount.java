package com.example.stock.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 500)
    private String roles;

    @Column(nullable = false)
    private boolean onboarded;

    /**
     * Creates an empty entity for JPA.
     */
    protected UserAccount() {
    }

    /**
     * Creates a persistent user account.
     *
     * @param email normalized account email address
     * @param passwordHash encoded account password
     * @param roles roles granted to the account
     * @param onboarded whether the account may authenticate
     */
    public UserAccount(String email, String passwordHash, Set<String> roles, boolean onboarded) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = serializeRoles(roles);
        this.onboarded = onboarded;
    }

    /**
     * Returns the normalized account email address.
     *
     * @return account email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the encoded account password.
     *
     * @return encoded password
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Returns the roles granted to the account.
     *
     * @return immutable account roles
     */
    public Set<String> getRoles() {
        return deserializeRoles(roles);
    }

    /**
     * Indicates whether the account may authenticate.
     *
     * @return {@code true} when onboarding is complete
     */
    public boolean isOnboarded() {
        return onboarded;
    }

    /**
     * Updates the account credentials, roles, and onboarding state.
     *
     * @param passwordHash encoded replacement password
     * @param roles replacement account roles
     * @param onboarded whether the account may authenticate
     */
    public void update(String passwordHash, Set<String> roles, boolean onboarded) {
        this.passwordHash = passwordHash;
        this.roles = serializeRoles(roles);
        this.onboarded = onboarded;
    }

    /**
     * Converts account roles to their persistent representation.
     *
     * @param roles account roles
     * @return sorted, comma-separated roles
     */
    private String serializeRoles(Set<String> roles) {
        return roles.stream().sorted().collect(Collectors.joining(","));
    }

    /**
     * Converts persisted roles to their domain representation.
     *
     * @param storedRoles comma-separated roles
     * @return immutable account roles
     */
    private Set<String> deserializeRoles(String storedRoles) {
        return Arrays.stream(storedRoles.split(","))
                .filter(role -> !role.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
