package com.example.retailstore.suppliers;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.shared.ui.NavigationPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
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
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Public view of product associations, with write controls for administrators. */
@Route("suppliers/:supplierId/products")
@AnonymousAllowed
public class SupplierProductsView extends VerticalLayout implements BeforeEnterObserver {

    private final SupplierService supplierService;
    private final ProductService productService;
    private final H2 heading = new H2("Supplier Products");
    private final Grid<OfferRow> grid = new Grid<>(OfferRow.class, false);
    private final ComboBox<Product> productField = new ComboBox<>("Product");
    private final TextField supplierSkuField = new TextField("Supplier SKU");
    private final IntegerField leadTimeField = new IntegerField("Lead time (days)");
    private @Nullable Long supplierId;

    /** Dependencies for the supplier's products and offers. */
    public SupplierProductsView(SupplierService supplierService, ProductService productService) {
        this.supplierService = supplierService;
        this.productService = productService;
        setSpacing(true);
        setPadding(true);
        grid.addColumn(row -> row.product().getSku()).setHeader("Product SKU").setSortable(true);
        grid.addColumn(row -> row.product().getName()).setHeader("Product").setSortable(true);
        grid.addColumn(row -> row.offer().getSupplierSku()).setHeader("Supplier SKU");
        grid.addColumn(row -> row.offer().getLeadTimeDays())
                .setHeader("Lead time (days)")
                .setSortable(true);
        grid.addComponentColumn(this::removeButton).setHeader("Actions");

        productField.setItemLabelGenerator(product -> product.getName() + " (" + product.getSku() + ")");
        productField.setItems(productService.listAll());
        productField.setRequiredIndicatorVisible(true);
        supplierSkuField.setMaxLength(100);
        supplierSkuField.setRequiredIndicatorVisible(true);
        supplierSkuField.setManualValidation(true);
        leadTimeField.setMin(1);
        leadTimeField.setStep(1);
        leadTimeField.setRequiredIndicatorVisible(true);
        leadTimeField.setManualValidation(true);

        Button addButton = new Button("Add product", event -> addProduct());
        VerticalLayout controls = new VerticalLayout(productField, supplierSkuField, leadTimeField, addButton);
        controls.setVisible(isAdmin());
        VerticalLayout content = new VerticalLayout(heading, new Anchor("/suppliers", "Suppliers"), grid, controls);
        content.setWidthFull();
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), content);
        layout.addClassName("supplier-layout");
        layout.setWidthFull();
        layout.setFlexGrow(1, content);
        add(layout);
    }

    /** Loads supplier offers, rerouting invalid identifiers or missing related data to not found. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get("supplierId").orElse(null);
        try {
            supplierId = Long.valueOf(Objects.requireNonNull(parameter));
            Supplier supplier = supplierService.findById(supplierId);
            heading.setText("Products supplied by " + supplier.getName());
            refresh();
        } catch (IllegalArgumentException | NullPointerException exception) {
            event.rerouteToError(NotFoundException.class);
        }
    }

    private Button removeButton(OfferRow row) {
        Button remove = new Button("Remove", event -> {
            if (supplierId != null) {
                supplierService.removeProduct(
                        supplierId, Objects.requireNonNull(row.product().getId()));
                refresh();
            }
        });
        remove.setVisible(isAdmin());
        return remove;
    }

    /**
     * Adds or updates the selected product offer, clears SKU and lead time, and reloads offers on success.
     * Displays incomplete input and service validation failures as notifications.
     */
    private void addProduct() {
        if (supplierId == null || productField.isEmpty() || supplierSkuField.isEmpty() || leadTimeField.isEmpty()) {
            Notification.show("Complete all product association fields.", 4000, Notification.Position.MIDDLE);
            return;
        }
        try {
            Long productId = Objects.requireNonNull(productField.getValue().getId());
            supplierService.associateProduct(
                    supplierId, productId, supplierSkuField.getValue(), leadTimeField.getValue());
            supplierSkuField.clear();
            leadTimeField.clear();
            refresh();
            Notification.show("Supplier product saved.");
        } catch (IllegalArgumentException exception) {
            Notification.show(exception.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void refresh() {
        if (supplierId == null) {
            return;
        }
        Map<Long, Product> products = productService.listAll().stream()
                .collect(Collectors.toMap(product -> Objects.requireNonNull(product.getId()), Function.identity()));
        List<OfferRow> offers = supplierService.productsForSupplier(supplierId).stream()
                .map(offer -> new OfferRow(
                        Objects.requireNonNull(products.get(offer.getProductId()), "Associated product must exist."),
                        offer))
                .toList();
        grid.setItems(offers);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private record OfferRow(Product product, ProductSupplier offer) {}
}
