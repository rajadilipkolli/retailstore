package com.example.stock.security;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("forgot-password")
@AnonymousAllowed
public class ForgotPasswordView extends VerticalLayout {

    public ForgotPasswordView(AccountService accountService) {
        addClassName("auth-view");
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        EmailField email = new EmailField("Email");
        email.setRequired(true);
        email.setWidthFull();

        Button submit = new Button("Send reset link", event -> {
            if (email.isEmpty() || !email.isInvalid()) {
                Notification.show(accountService.requestPasswordReset(email.getValue()));
            }
        });
        submit.addClassName("primary-action");

        add(new H2("Reset your password"), new Paragraph("Enter your email and we will send a reset link if an account exists."), email, submit);
    }
}