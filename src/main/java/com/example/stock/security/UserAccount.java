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

    protected UserAccount() {
    }

    public UserAccount(String email, String passwordHash, Set<String> roles, boolean onboarded) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = serializeRoles(roles);
        this.onboarded = onboarded;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<String> getRoles() {
        return deserializeRoles(roles);
    }

    public boolean isOnboarded() {
        return onboarded;
    }

    public void update(String passwordHash, Set<String> roles, boolean onboarded) {
        this.passwordHash = passwordHash;
        this.roles = serializeRoles(roles);
        this.onboarded = onboarded;
    }

    private String serializeRoles(Set<String> roles) {
        return roles.stream().sorted().collect(Collectors.joining(","));
    }

    private Set<String> deserializeRoles(String storedRoles) {
        return Arrays.stream(storedRoles.split(","))
                .filter(role -> !role.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}