package com.example.retailstore.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("reset-password")
@AnonymousAllowed
public class ResetPasswordView extends VerticalLayout implements BeforeEnterObserver {

    private final AccountService accountService;
    private final PasswordField password;
    private final PasswordField confirmation;
    private final Button submit;
    private final Paragraph invalidLinkMessage;
    private String token = "";

    /**
     * Creates the form used to replace a password from a reset link.
     *
     * @param accountService account service that validates reset requests
     */
    public ResetPasswordView(AccountService accountService) {
        this.accountService = accountService;
        addClassName("auth-view");
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        password = new PasswordField("New password");
        confirmation = new PasswordField("Confirm new password");
        password.setMinLength(8);
        password.setRequired(true);
        confirmation.setRequired(true);

        submit = new Button("Reset password", event -> {
            if (password.isInvalid()
                    || confirmation.isInvalid()
                    || !password.getValue().equals(confirmation.getValue())) {
                confirmation.setErrorMessage("Passwords must match and meet the password policy.");
                confirmation.setInvalid(true);
                return;
            }
            if (!accountService.resetPassword(token, password.getValue())) {
                Notification.show("This reset link is invalid or expired. Request a new link.");
                return;
            }
            UI.getCurrent().navigate(LoginView.class);
        });

        invalidLinkMessage = new Paragraph("This reset link is invalid or expired. Request a new link.");
        invalidLinkMessage.addClassName("auth-error");
        invalidLinkMessage.setVisible(false);

        Div panel = new Div();
        panel.addClassName("auth-panel");
        panel.add(new H2("Choose a new password"), invalidLinkMessage, password, confirmation, submit);
        add(panel);
    }

    /**
     * Reads the first reset token from the route query parameters. Missing or unusable tokens
     * display an error and disable the password fields and submit button.
     *
     * @param event navigation event containing the reset link parameters
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        token =
                event
                        .getLocation()
                        .getQueryParameters()
                        .getParameters()
                        .getOrDefault("token", java.util.List.of(""))
                        .stream()
                        .findFirst()
                        .orElse("");
        boolean usable = accountService.isResetTokenUsable(token);
        invalidLinkMessage.setVisible(!usable);
        password.setEnabled(usable);
        confirmation.setEnabled(usable);
        submit.setEnabled(usable);
    }
}
