package com.example.retailstore.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccountServiceBootstrapTest {

    @Test
    void bootstrapRequiresPasswordChangeAndDoesNotReplaceExistingCredentials() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        AtomicReference<UserAccount> stored = new AtomicReference<>();
        AtomicReference<String> resetLink = new AtomicReference<>();
        AtomicInteger resetMessages = new AtomicInteger();
        when(repository.findByEmail("admin@retailstore.com"))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount account = invocation.getArgument(0);
            stored.set(account);
            return account;
        });
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        AccountService service = new AccountService(
                encoder,
                (recipient, url) -> {
                    resetLink.set(url);
                    resetMessages.incrementAndGet();
                },
                repository,
                "http://localhost:8080/reset-password",
                Clock.systemUTC());

        service.bootstrapAdmin();

        UserAccount admin = stored.get();
        assertThat(admin.getRoles()).contains("ADMIN");
        assertThatThrownBy(() -> service.loadUserByUsername("admin@retailstore.com"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThat(resetLink.get()).startsWith("http://localhost:8080/reset-password?token=");
        String initialPasswordHash = admin.getPasswordHash();
        service.bootstrapAdmin();
        assertThat(stored.get().getPasswordHash()).isEqualTo(initialPasswordHash);
        assertThat(resetMessages).hasValue(1);

        String token = service.latestResetTokenFor("admin@retailstore.com").orElseThrow();
        assertThat(service.resetPassword(token, "replacement-password")).isTrue();
        assertThat(service.loadUserByUsername("admin@retailstore.com").getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_ADMIN");
        String passwordHash = admin.getPasswordHash();
        service.bootstrapAdmin();

        assertThat(stored.get().getPasswordHash()).isEqualTo(passwordHash);
        assertThat(encoder.matches("replacement-password", stored.get().getPasswordHash()))
                .isTrue();
        assertThat(resetMessages).hasValue(1);
        verify(repository, times(2)).save(any(UserAccount.class));
    }
}
