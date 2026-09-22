package com.example.stock.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

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

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/forgot-password", "/reset-password", "/styles.css").permitAll()
                .requestMatchers("/home").authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/home", true).permitAll())
                .rememberMe(remember -> remember.rememberMeParameter("remember-me"))
                .logout(logout -> logout.logoutSuccessUrl("/login"));
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
        });
        return http.build();
    }
}