package com.example.retailstore.security;

import java.util.Set;

public record Account(String email, String passwordHash, Set<String> roles, boolean onboarded) {}
