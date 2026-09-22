package com.example.stock.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    public LoginView() {
        addClassName("login-view");
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        Div panel = new Div();
        panel.addClassName("login-panel");

        LoginForm loginForm = new LoginForm();
        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(true);
        loginForm.addForgotPasswordListener(event -> UI.getCurrent().navigate(ForgotPasswordView.class));
        LoginI18n loginI18n = LoginI18n.createDefault();
        loginI18n.getForm().setUsername("Email");
        loginForm.setI18n(loginI18n);

        Checkbox rememberMe = new Checkbox("Remember me");
        rememberMe.setId("remember-me");

        panel.add(loginForm, rememberMe);
        add(panel);
    }
}