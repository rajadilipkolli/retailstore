package com.example.retailstore.alerts;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.shared.ui.NavigationPanel;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Public alert action route; only administrators can acknowledge or reopen an alert. */
@Route("alerts/:alertId/acknowledge")
@AnonymousAllowed
public class LowStockAlertActionView extends VerticalLayout implements BeforeEnterObserver {

    private final LowStockAlertService alertService;
    private final ProductService productService;
    private final VerticalLayout content = new VerticalLayout();
    private @Nullable Long alertId;
    private @Nullable LowStockAlert alert;

    /** Dependencies for alert state and its product details. */
    public LowStockAlertActionView(LowStockAlertService alertService, ProductService productService) {
        this.alertService = alertService;
        this.productService = productService;
        setSpacing(true);
        setPadding(true);
        content.setWidthFull();
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), content);
        layout.addClassName("alert-layout");
        layout.setWidthFull();
        layout.setFlexGrow(1, content);
        add(layout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get("alertId").orElse(null);
        try {
            alertId = Long.valueOf(Objects.requireNonNull(parameter));
            alert = alertService.findById(alertId);
            showAction(alert);
        } catch (IllegalArgumentException | NullPointerException exception) {
            event.rerouteToError(NotFoundException.class);
        }
    }

    private void showAction(LowStockAlert currentAlert) {
        Product product = productService.findById(currentAlert.getProductId());
        content.removeAll();
        content.add(new H2(currentAlert.isAcknowledged() ? "Reopen alert" : "Acknowledge alert"));
        content.add(new Paragraph(product.getName() + " (" + product.getSku() + ")"));
        if (currentAlert.isResolved()) {
            content.add(new Paragraph("Stock has been replenished; this alert is resolved."));
        } else if (isAdmin()) {
            Button action = new Button(currentAlert.isAcknowledged() ? "Reopen alert" : "Acknowledge alert");
            action.addClickListener(event -> performAction());
            content.add(action);
        } else {
            content.add(new Paragraph("Only an administrator can change alert review status."));
        }
        Long id = Objects.requireNonNull(currentAlert.getId());
        content.add(new Anchor("/alerts/" + id, "Back to alert"));
    }

    private void performAction() {
        if (alertId == null || alert == null) {
            return;
        }
        try {
            if (alert.isAcknowledged()) {
                alertService.reopen(alertId);
            } else {
                alertService.acknowledge(alertId);
            }
            UI.getCurrent().navigate("alerts/" + alertId);
        } catch (IllegalArgumentException exception) {
            Notification.show(exception.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
