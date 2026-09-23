package com.example.stock.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /**
     * Finds an account by its normalized email address.
     *
     * @param email normalized account email address
     * @return the matching account, when present
     */
    Optional<UserAccount> findByEmail(String email);
}
