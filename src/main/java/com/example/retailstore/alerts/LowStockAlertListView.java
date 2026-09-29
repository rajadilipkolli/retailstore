package com.example.retailstore.alerts;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.shared.ui.NavigationPanel;
import com.example.retailstore.stock.Stock;
import com.example.retailstore.stock.StockService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Public list of low-stock alerts waiting for review. */
@Route("alerts")
@AnonymousAllowed
public class LowStockAlertListView extends VerticalLayout {

    private final LowStockAlertService alertService;
    private final ProductService productService;
    private final StockService stockService;
    private final Grid<AlertRow> grid = new Grid<>(AlertRow.class, false);

    /** Dependencies for current alerts and their product/stock details. */
    public LowStockAlertListView(
            LowStockAlertService alertService, ProductService productService, StockService stockService) {
        this.alertService = alertService;
        this.productService = productService;
        this.stockService = stockService;
        setSpacing(true);
        setPadding(true);
        VerticalLayout content =
                new VerticalLayout(new H2("Low-Stock Alerts"), new Anchor("/alerts/history", "Alert history"), grid);
        content.setWidthFull();
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), content);
        layout.addClassName("alert-layout");
        layout.setWidthFull();
        layout.setFlexGrow(1, content);
        add(layout);
        configureGrid();
        refresh();
    }

    private void configureGrid() {
        grid.addColumn(row -> row.product().getName()).setHeader("Product").setSortable(true);
        grid.addColumn(row -> row.product().getSku()).setHeader("SKU").setSortable(true);
        grid.addColumn(row -> row.stock().getQuantityOnHand())
                .setHeader("Current quantity")
                .setSortable(true);
        grid.addColumn(row -> row.product().getReorderLevel())
                .setHeader("Reorder level")
                .setSortable(true);
        grid.addColumn(row -> row.stock().getLastUpdated())
                .setHeader("Last stock update")
                .setSortable(true);
        grid.addColumn(row -> row.alert().getAlertedAt()).setHeader("Alerted").setSortable(true);
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                Long alertId = Objects.requireNonNull(event.getValue().alert().getId());
                UI.getCurrent().navigate("alerts/" + alertId);
            }
        });
    }

    private void refresh() {
        List<LowStockAlert> alerts = alertService.activeAlerts();
        List<Long> productIds =
                alerts.stream().map(LowStockAlert::getProductId).distinct().toList();
        Map<Long, Product> products = productService.findByIds(productIds);
        Map<Long, Stock> stocks = stockService.findByProductIds(productIds);
        List<AlertRow> rows = alerts.stream()
                .map(alert -> new AlertRow(
                        alert,
                        Objects.requireNonNull(products.get(alert.getProductId())),
                        Objects.requireNonNull(stocks.get(alert.getProductId()))))
                .toList();
        grid.setItems(rows);
    }

    private record AlertRow(LowStockAlert alert, Product product, Stock stock) {}
}
