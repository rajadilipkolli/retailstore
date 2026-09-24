package com.example.retailstore;

import com.example.retailstore.security.AccountService;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.aura.Aura;
import java.util.Set;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css") // Your custom styles
@Push
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Creates or updates the local development administrator account.
     *
     * @param accountService service used to provision the administrator account
     * @return the runner that provisions the administrator on startup
     */
    @Bean
    ApplicationRunner seedDefaultAdmin(AccountService accountService) {
        return args -> accountService.onboard("admin@retailstore.com", "AbcXyz@123", Set.of("ADMIN"));
    }
}
