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
     * Creates a startup runner that onboards the default administrator. Each run replaces the
     * account's password and roles if it already exists.
     *
     * @param accountService service used to create or update the administrator account
     * @return the runner that onboards the administrator at startup
     */
    @Bean
    ApplicationRunner seedDefaultAdmin(AccountService accountService) {
        return args -> accountService.onboard("admin@retailstore.com", "Admin@1234", Set.of("ADMIN"));
    }
}
