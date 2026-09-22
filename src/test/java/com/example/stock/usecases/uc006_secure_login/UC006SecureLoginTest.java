package com.example.stock.usecases.uc006_secure_login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import com.example.stock.security.AccountService;
import com.example.stock.security.EmailSender;
import com.example.stock.security.UserAccount;
import com.example.stock.security.UserAccountRepository;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UC006SecureLoginTest {

    @Container
    private static final GenericContainer<?> mailpit = new GenericContainer<>("axllent/mailpit:v1.21.8")
            .withExposedPorts(1025, 8025);

    @DynamicPropertySource
    static void mailpitProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:secure_login_test;DB_CLOSE_DELAY=-1");
        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
        registry.add("app.mail.reset-url", () -> "http://localhost:8080/reset-password");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserAccountRepository accountRepository;

    @Test
    void mainFlow_unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void mainFlow_validOnboardedUserIsRedirectedToHome() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "manager@example.com")
                .param("password", "password")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void af1_invalidCredentialsRemainUnauthenticated() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "manager@example.com")
                .param("password", "incorrect")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void br03_rememberMeCreatesPersistentLoginCookie() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "manager@example.com")
                .param("password", "password")
                .param("remember-me", "on")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(cookie().exists("remember-me"));
    }

    @Test
    void af2_passwordRecoveryReturnsNeutralResponseForUnknownEmail() {
        String response = accountService.requestPasswordReset("unknown@example.com");

        assertThat(response).isEqualTo("If an account exists for that email, a reset link has been sent.");
    }

    @Test
    void af2_passwordRecoveryCreatesTokenForKnownEmail() {
        accountService.requestPasswordReset("manager@example.com");

        assertThat(accountService.latestResetTokenFor("manager@example.com")).isPresent();
    }

    @Test
    void af2_passwordRecoverySendsResetEmailThroughMailpit() throws Exception {
        accountService.requestPasswordReset("manager@example.com");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025) + "/api/v1/messages"))
                .GET()
                .build();
        String messages = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();

        assertThat(messages).contains("manager@example.com", "Reset your Retail Store password",
                "/reset-password?token=");
    }

    @Test
    void af6_emailDeliveryFailureReturnsNeutralResponseAndInvalidatesToken() {
        EmailSender failingEmailSender = (recipient, resetUrl) -> {
            throw new IllegalStateException("Mailpit unavailable");
        };
        AccountService service = new AccountService(new BCryptPasswordEncoder(), failingEmailSender, accountRepository,
            "http://localhost:8080/reset-password", Clock.systemUTC());

        assertThat(service.requestPasswordReset("manager@example.com"))
                .isEqualTo("If an account exists for that email, a reset link has been sent.");
        assertThat(service.latestResetTokenFor("manager@example.com")).isEmpty();
    }

    @Test
    void af3_resetPasswordInvalidatesTokenAfterUse() {
        accountService.requestPasswordReset("manager@example.com");
        String token = accountService.latestResetTokenFor("manager@example.com").orElseThrow();

        assertThat(accountService.resetPassword(token, "new-password")).isTrue();
        assertThat(accountService.resetPassword(token, "another-password")).isFalse();
    }

    @Test
    void af4_invalidResetTokenDoesNotChangePassword() {
        assertThat(accountService.resetPassword("invalid-token", "new-password")).isFalse();
    }

    @Test
    void af4_expiredResetTokenDoesNotChangePassword() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-22T20:00:00Z"));
        AccountService service = new AccountService(new BCryptPasswordEncoder(), (recipient, resetUrl) -> {
        }, accountRepository, "http://localhost:8080/reset-password", clock);
        service.requestPasswordReset("manager@example.com");
        String token = service.latestResetTokenFor("manager@example.com").orElseThrow();

        clock.advance(Duration.ofMinutes(31));

        assertThat(service.resetPassword(token, "new-password")).isFalse();
    }

    @Test
    void af5_invalidNewPasswordIsRejected() {
        accountService.requestPasswordReset("manager@example.com");
        String token = accountService.latestResetTokenFor("manager@example.com").orElseThrow();

        assertThat(accountService.resetPassword(token, "short")).isFalse();
    }

    @Test
    void br01_emailIsCaseInsensitiveIdentity() {
        accountService.onboard("Unique@Example.com", "password", Set.of("WAREHOUSE_STAFF"));

        assertThat(accountService.loadUserByUsername("unique@example.com").getUsername())
                .isEqualTo("unique@example.com");
    }

    @Test
    void br01_duplicateEmailRegistrationIsRejected() {
        assertThat(accountService.register("manager@example.com", "password", Set.of("WAREHOUSE_STAFF")))
                .isFalse();
    }

    @Test
    void br02_nonOnboardedAccountsCannotAuthenticate() {
        accountService.register("pending@example.com", "password", Set.of("WAREHOUSE_STAFF"));

        assertThat(accountRepository.findByEmail("pending@example.com"))
            .isPresent()
            .get()
            .extracting(UserAccount::isOnboarded)
            .isEqualTo(false);
        assertThatThrownBy(() -> accountService.loadUserByUsername("pending@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void br02_registrationPersistsPendingAccountUntilOnboarding() {
        assertThat(accountService.register("new-user@example.com", "password", Set.of("WAREHOUSE_STAFF")))
                .isTrue();
        assertThat(accountRepository.findByEmail("new-user@example.com"))
                .isPresent()
                .get()
                .extracting(UserAccount::isOnboarded)
                .isEqualTo(false);
    }

    @Test
    void br02_adminOnboardingEnablesAuthentication() {
        accountService.register("approved@example.com", "password", Set.of("WAREHOUSE_STAFF"));

        accountService.onboard("approved@example.com", "password", Set.of("WAREHOUSE_STAFF"));

        assertThat(accountService.loadUserByUsername("approved@example.com").getUsername())
                .isEqualTo("approved@example.com");
    }

    @Test
    void af3_successfulResetChangesPassword() {
        accountService.requestPasswordReset("manager@example.com");
        String token = accountService.latestResetTokenFor("manager@example.com").orElseThrow();

        assertThat(accountService.resetPassword(token, "updated-password")).isTrue();
        assertThat(accountService.loadUserByUsername("manager@example.com").getPassword())
                .isNotEqualTo("password");
    }

    @Test
    void br05_passwordResetRequiresAtLeastEightCharacters() {
        accountService.requestPasswordReset("manager@example.com");
        String token = accountService.latestResetTokenFor("manager@example.com").orElseThrow();

        assertThat(accountService.resetPassword(token, "1234567")).isFalse();
    }

    private static final class MutableClock extends Clock {

        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}