package com.example.retailstore.suppliers;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.shared.ui.NavigationPanel;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Public supplier contact details and associated products. */
@Route("suppliers/:supplierId")
@AnonymousAllowed
public class SupplierDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final SupplierService supplierService;
    private final ProductService productService;
    private final VerticalLayout content = new VerticalLayout();
    private final Grid<OfferRow> productsGrid = new Grid<>(OfferRow.class, false);

    /** Dependencies for supplier and product information. */
    public SupplierDetailView(SupplierService supplierService, ProductService productService) {
        this.supplierService = supplierService;
        this.productService = productService;
        setSpacing(true);
        setPadding(true);
        content.setWidthFull();
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), content);
        layout.addClassName("supplier-layout");
        layout.setWidthFull();
        layout.setFlexGrow(1, content);
        add(layout);
        productsGrid
                .addColumn(row -> row.product().getSku())
                .setHeader("Product SKU")
                .setSortable(true);
        productsGrid
                .addColumn(row -> row.product().getName())
                .setHeader("Product")
                .setSortable(true);
        productsGrid.addColumn(row -> row.offer().getSupplierSku()).setHeader("Supplier SKU");
        productsGrid
                .addColumn(row -> row.offer().getLeadTimeDays())
                .setHeader("Lead time (days)")
                .setSortable(true);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get("supplierId").orElse(null);
        try {
            Long supplierId = Long.valueOf(Objects.requireNonNull(parameter));
            Supplier supplier = supplierService.findById(supplierId);
            showSupplier(supplier);
        } catch (IllegalArgumentException | NullPointerException exception) {
            event.rerouteToError(NotFoundException.class);
        }
    }

    private void showSupplier(Supplier supplier) {
        Long id = Objects.requireNonNull(supplier.getId());
        content.removeAll();
        content.add(new H2(supplier.getName()));
        content.add(new Paragraph("Contact person: " + supplier.getContactPerson()));
        if (!supplier.getEmail().isBlank()) {
            content.add(new Paragraph("Email: " + supplier.getEmail()));
        }
        if (!supplier.getPhone().isBlank()) {
            content.add(new Paragraph("Phone: " + supplier.getPhone()));
        }
        if (!supplier.getAddress().isBlank()) {
            content.add(new Paragraph("Address: " + supplier.getAddress()));
        }
        if (!supplier.getNotes().isBlank()) {
            content.add(new Paragraph("Notes: " + supplier.getNotes()));
        }
        if (isAdmin()) {
            Button edit = new Button("Edit supplier", event -> UI.getCurrent().navigate("suppliers/" + id + "/edit"));
            Anchor manageProducts = new Anchor("/suppliers/" + id + "/products", "Manage products");
            content.add(new HorizontalLayout(edit, manageProducts));
        }
        content.add(new H3("Products supplied"), productsGrid);
        Map<Long, Product> products = productService.listAll().stream()
                .collect(Collectors.toMap(product -> Objects.requireNonNull(product.getId()), Function.identity()));
        List<OfferRow> offers = supplierService.productsForSupplier(id).stream()
                .map(offer -> new OfferRow(
                        Objects.requireNonNull(products.get(offer.getProductId()), "Associated product must exist."),
                        offer))
                .toList();
        productsGrid.setItems(offers);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private record OfferRow(Product product, ProductSupplier offer) {}
}
