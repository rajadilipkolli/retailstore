package com.example.stock.security;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("register")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    /**
     * Creates the pending-account registration form.
     *
     * @param accountService account service that registers submitted accounts
     */
    public RegisterView(AccountService accountService) {
        addClassName("auth-view");
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        EmailField email = new EmailField("Email");
        PasswordField password = new PasswordField("Password");
        PasswordField confirmation = new PasswordField("Confirm password");
        email.setRequired(true);
        password.setRequired(true);
        password.setMinLength(8);
        confirmation.setRequired(true);

        Button submit = new Button("Create account", event -> {
            if (email.isInvalid() || password.isInvalid() || confirmation.isInvalid()
                    || !password.getValue().equals(confirmation.getValue())) {
                confirmation.setErrorMessage("Passwords must match and meet the password policy.");
                confirmation.setInvalid(true);
                return;
            }
            if (!accountService.register(email.getValue(), password.getValue(), java.util.Set.of("WAREHOUSE_STAFF"))) {
                Notification.show("An account already exists for that email.");
                return;
            }
            Notification.show("Registration submitted for administrator approval.");
        });
        submit.addClassName("primary-action");

        Div panel = new Div();
        panel.addClassName("auth-panel");
        panel.add(new H2("Create an account"),
                new Paragraph("An administrator must approve your account before you can sign in."),
                email, password, confirmation, submit);
        add(panel);
    }
}
