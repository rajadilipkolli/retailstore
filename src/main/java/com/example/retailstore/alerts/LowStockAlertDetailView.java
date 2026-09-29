package com.example.retailstore.alerts;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.shared.ui.NavigationPanel;
import com.example.retailstore.stock.Stock;
import com.example.retailstore.stock.StockService;
import com.example.retailstore.suppliers.ProductSupplier;
import com.example.retailstore.suppliers.Supplier;
import com.example.retailstore.suppliers.SupplierService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Public alert details and supplier purchasing options. */
@Route("alerts/:alertId")
@AnonymousAllowed
public class LowStockAlertDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final LowStockAlertService alertService;
    private final ProductService productService;
    private final StockService stockService;
    private final SupplierService supplierService;
    private final VerticalLayout content = new VerticalLayout();

    /** Dependencies for alert state, catalog, stock, and supplier details. */
    public LowStockAlertDetailView(
            LowStockAlertService alertService,
            ProductService productService,
            StockService stockService,
            SupplierService supplierService) {
        this.alertService = alertService;
        this.productService = productService;
        this.stockService = stockService;
        this.supplierService = supplierService;
        setSpacing(true);
        setPadding(true);
        content.setWidthFull();
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), content);
        layout.addClassName("alert-layout");
        layout.setWidthFull();
        layout.setFlexGrow(1, content);
        add(layout);
    }

    /** Loads alert details, rerouting invalid identifiers or missing related data to not found. */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get("alertId").orElse(null);
        try {
            Long alertId = Long.valueOf(Objects.requireNonNull(parameter));
            showAlert(alertService.findById(alertId));
        } catch (IllegalArgumentException | NullPointerException exception) {
            event.rerouteToError(NotFoundException.class);
        }
    }

    private void showAlert(LowStockAlert alert) {
        Product product = productService.findById(alert.getProductId());
        Stock stock = stockService.findByProductId(alert.getProductId());
        content.removeAll();
        content.add(
                new H2(product.getName()),
                new Paragraph("SKU: " + product.getSku()),
                new Paragraph("Current quantity: " + stock.getQuantityOnHand()),
                new Paragraph("Reorder level: " + product.getReorderLevel()),
                new Paragraph("Last stock update: " + stock.getLastUpdated()),
                new Paragraph("Alerted: " + alert.getAlertedAt()),
                new Paragraph("Status: " + status(alert)));
        if (isAdmin() && !alert.isResolved()) {
            Long alertId = Objects.requireNonNull(alert.getId());
            String label = alert.isAcknowledged() ? "Reopen alert" : "Acknowledge alert";
            Button reviewButton =
                    new Button(label, click -> UI.getCurrent().navigate("alerts/" + alertId + "/acknowledge"));
            content.add(reviewButton);
        }
        content.add(new H3("Supplier options"), supplierGrid(product));
        content.add(new Anchor(
                alert.isAcknowledged() || alert.isResolved() ? "/alerts/history" : "/alerts", "Back to alerts"));
    }

    /** Builds the offers grid in lead-time order, marking only the first offer as suggested. */
    private Grid<SupplierOfferRow> supplierGrid(Product product) {
        Grid<SupplierOfferRow> grid = new Grid<>(SupplierOfferRow.class, false);
        grid.addColumn(row -> row.supplier().getName()).setHeader("Supplier").setSortable(true);
        grid.addColumn(row -> row.offer().getSupplierSku()).setHeader("Supplier SKU");
        grid.addColumn(row -> row.offer().getLeadTimeDays())
                .setHeader("Lead time (days)")
                .setSortable(true);
        grid.addColumn(row -> row.suggested() ? "Suggested" : "").setHeader("Recommendation");

        List<ProductSupplier> offers = alertService.supplierOptions(Objects.requireNonNull(product.getId()));
        List<SupplierOfferRow> rows = new ArrayList<>();
        for (int index = 0; index < offers.size(); index++) {
            ProductSupplier offer = offers.get(index);
            Supplier supplier = supplierService.findById(offer.getSupplierId());
            rows.add(new SupplierOfferRow(supplier, offer, index == 0));
        }
        grid.setItems(rows);
        return grid;
    }

    private String status(LowStockAlert alert) {
        if (alert.isResolved()) {
            return alert.isAcknowledged() ? "Reviewed, replenished" : "Replenished";
        }
        return alert.isAcknowledged() ? "Reviewed, still low" : "Needs review";
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private record SupplierOfferRow(Supplier supplier, ProductSupplier offer, boolean suggested) {}
}
