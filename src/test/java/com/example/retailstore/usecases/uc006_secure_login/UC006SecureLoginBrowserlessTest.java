package com.example.retailstore.usecases.uc006_secure_login;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.retailstore.security.LoginView;
import com.example.retailstore.usecases.TestcontainersConfig;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.login.LoginForm;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class UC006SecureLoginBrowserlessTest extends SpringBrowserlessTest {

    @Test
    @WithAnonymousUser
    void mainFlow_loginViewRendersRequiredControls() {
        navigate(LoginView.class);

        LoginForm loginForm = find(LoginForm.class).single();
        Checkbox rememberMe = find(Checkbox.class).single();

        assertThat(loginForm.getAction()).isEqualTo("login");
        assertThat(loginForm.isForgotPasswordButtonVisible()).isTrue();
        assertThat(rememberMe.getLabel()).isEqualTo("Remember me");
        assertThat(rememberMe.getId()).contains("remember-me");
    }
}
