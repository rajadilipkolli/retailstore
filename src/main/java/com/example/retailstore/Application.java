package com.example.retailstore;

import com.example.retailstore.security.AccountService;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.aura.Aura;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css") // Your custom styles
@Push
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Creates the local development administrator account if it does not exist.
     *
     * @param accountService service used to provision the administrator account
     * @return the runner that provisions the administrator on startup
     */
    @Bean
    @Profile("dev")
    ApplicationRunner seedDefaultAdmin(AccountService accountService, @Value("${app.admin.password}") String password) {
        if (password.isBlank()) {
            throw new IllegalArgumentException("Development administrator password is required.");
        }
        return args -> accountService.onboardIfAbsent("admin@retailstore.com", password, Set.of("ADMIN"));
    }
}
