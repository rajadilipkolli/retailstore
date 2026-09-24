package com.example.retailstore.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    /**
     * Provides the password encoder used for stored account credentials.
     *
     * @return a BCrypt password encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provides the time source used by expiring security operations.
     *
     * @return the system clock in UTC
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Configures public authentication routes and protected application routes.
     *
     * @param http Spring Security HTTP configuration
     * @return the configured security filter chain
     * @throws Exception when the security configuration cannot be built
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/login", "/register", "/forgot-password", "/reset-password", "/styles.css")
                        .permitAll()
                        .requestMatchers("/home")
                        .authenticated())
                .rememberMe(remember -> remember.rememberMeParameter("remember-me"))
                .logout(logout -> logout.logoutSuccessUrl("/login"));
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            configurer.loginView(LoginView.class).defaultSuccessUrl("/home", true);
        });
        return http.build();
    }
}
