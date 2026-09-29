package com.example.retailstore.suppliers;

import com.example.retailstore.shared.ui.NavigationPanel;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** ADMIN-only supplier creation and editing form. */
@Route("suppliers/new")
@RouteAlias("suppliers/:supplierId/edit")
@RolesAllowed("ADMIN")
public class SupplierFormView extends VerticalLayout implements BeforeEnterObserver {

    private final SupplierService supplierService;
    private final H2 heading = new H2("Add Supplier");
    private final TextField nameField = new TextField("Supplier name");
    private final TextField contactPersonField = new TextField("Contact person");
    private final EmailField emailField = new EmailField("Email");
    private final TextField phoneField = new TextField("Phone");
    private final TextArea addressField = new TextArea("Address");
    private final TextArea notesField = new TextArea("Notes");
    private @Nullable Long supplierId;

    /** @param supplierService validates and persists supplier details */
    public SupplierFormView(SupplierService supplierService) {
        this.supplierService = supplierService;
        setSpacing(true);
        setPadding(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setMaxLength(200);
        contactPersonField.setMaxLength(200);
        emailField.setMaxLength(320);
        phoneField.setMaxLength(50);
        addressField.setMaxLength(1000);
        notesField.setMaxLength(2000);
        Button saveButton = new Button("Save supplier", event -> save());
        VerticalLayout form = new VerticalLayout(
                heading,
                nameField,
                contactPersonField,
                emailField,
                phoneField,
                addressField,
                notesField,
                new HorizontalLayout(saveButton, new Anchor("/suppliers", "Cancel")));
        form.setWidthFull();
        form.setMaxWidth("var(--aura-size-l)");
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), form);
        layout.addClassName("supplier-layout");
        layout.setWidthFull();
        layout.setFlexGrow(1, form);
        add(layout);
    }

    /**
     * Selects creation mode when no supplier identifier is supplied; otherwise loads the supplier for editing.
     * Reroutes invalid identifiers and missing suppliers to not found.
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get("supplierId").orElse(null);
        if (parameter == null) {
            supplierId = null;
            return;
        }
        try {
            supplierId = Long.valueOf(parameter);
            Supplier supplier = supplierService.findById(supplierId);
            heading.setText("Edit Supplier");
            nameField.setValue(supplier.getName());
            contactPersonField.setValue(supplier.getContactPerson());
            emailField.setValue(supplier.getEmail());
            phoneField.setValue(supplier.getPhone());
            addressField.setValue(supplier.getAddress());
            notesField.setValue(supplier.getNotes());
        } catch (IllegalArgumentException exception) {
            event.rerouteToError(NotFoundException.class);
        }
    }

    /** Saves the current fields and navigates to supplier details, displaying validation failures as notifications. */
    private void save() {
        try {
            Supplier saved = supplierId == null
                    ? supplierService.create(
                            nameField.getValue(),
                            contactPersonField.getValue(),
                            emailField.getValue(),
                            phoneField.getValue(),
                            addressField.getValue(),
                            notesField.getValue())
                    : supplierService.update(
                            supplierId,
                            nameField.getValue(),
                            contactPersonField.getValue(),
                            emailField.getValue(),
                            phoneField.getValue(),
                            addressField.getValue(),
                            notesField.getValue());
            Long savedId = Objects.requireNonNull(saved.getId());
            Notification.show("Supplier saved.");
            UI.getCurrent().navigate("suppliers/" + savedId);
        } catch (IllegalArgumentException exception) {
            Notification.show(exception.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }
}
