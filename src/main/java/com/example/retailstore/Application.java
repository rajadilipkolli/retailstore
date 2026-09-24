package com.example.retailstore;

import com.example.retailstore.security.AccountService;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.aura.Aura;
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
     * Creates the default administrator only on first startup.
     *
     * @param accountService service used to bootstrap the administrator account
     * @return the runner that provisions the administrator when absent
     */
    @Bean
    ApplicationRunner seedDefaultAdmin(AccountService accountService) {
        return args -> accountService.bootstrapAdmin();
    }
}
