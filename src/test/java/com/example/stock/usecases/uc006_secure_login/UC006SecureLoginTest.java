package com.example.stock.usecases.uc006_secure_login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import com.example.stock.security.AccountService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UC006SecureLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

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
    void af3_resetPasswordInvalidatesTokenAfterUse() {
        accountService.requestPasswordReset("manager@example.com");
        String token = accountService.latestResetTokenFor("manager@example.com").orElseThrow();

        assertThat(accountService.resetPassword(token, "new-password")).isTrue();
        assertThat(accountService.resetPassword(token, "another-password")).isFalse();
    }

    @Test
    void br01_emailIsCaseInsensitiveIdentity() {
        accountService.onboard("Unique@Example.com", "password", Set.of("WAREHOUSE_STAFF"));

        assertThat(accountService.loadUserByUsername("unique@example.com").getUsername())
                .isEqualTo("unique@example.com");
    }

    @Test
    void br02_nonOnboardedAccountsCannotAuthenticate() {
        accountService.register("pending@example.com", "password", Set.of("WAREHOUSE_STAFF"));

        assertThatThrownBy(() -> accountService.loadUserByUsername("pending@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}