package com.example.retailstore.suppliers;

import com.example.retailstore.shared.ui.NavigationPanel;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Public supplier directory with search and detail navigation. */
@Route("suppliers")
@AnonymousAllowed
public class SupplierListView extends VerticalLayout {

    private final SupplierService supplierService;
    private final Grid<Supplier> grid = new Grid<>(Supplier.class, false);
    private final TextField searchField = new TextField("Search suppliers");

    /** @param supplierService reads supplier information */
    public SupplierListView(SupplierService supplierService) {
        this.supplierService = supplierService;
        setSpacing(true);
        setPadding(true);
        VerticalLayout content = new VerticalLayout(new H2("Suppliers"), toolbar(), searchField, grid);
        content.setWidthFull();
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), content);
        layout.addClassName("supplier-layout");
        layout.setWidthFull();
        layout.setFlexGrow(1, content);
        add(layout);

        grid.addColumn(Supplier::getName).setHeader("Supplier").setSortable(true);
        grid.addColumn(Supplier::getContactPerson).setHeader("Contact person").setSortable(true);
        grid.addColumn(Supplier::getEmail).setHeader("Email");
        grid.addColumn(Supplier::getPhone).setHeader("Phone");
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                Long id = Objects.requireNonNull(event.getValue().getId());
                UI.getCurrent().navigate("suppliers/" + id);
            }
        });

        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(event -> refresh(event.getValue()));
        refresh("");
    }

    private HorizontalLayout toolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.add(new Anchor("/suppliers/new", "Add Supplier"));
        toolbar.getChildren().findFirst().orElseThrow().setVisible(isAdmin());
        return toolbar;
    }

    private void refresh(String query) {
        List<Supplier> suppliers = supplierService.search(query);
        grid.setItems(suppliers);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
