package com.example.retailstore.alerts;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.shared.ui.NavigationPanel;
import com.example.retailstore.stock.Stock;
import com.example.retailstore.stock.StockService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Public history of acknowledged and resolved low-stock alerts. */
@Route("alerts/history")
@AnonymousAllowed
public class LowStockAlertHistoryView extends VerticalLayout {

    private final LowStockAlertService alertService;
    private final ProductService productService;
    private final StockService stockService;
    private final Grid<HistoryRow> grid = new Grid<>(HistoryRow.class, false);
    private final DatePicker alertedOnField = new DatePicker("Alerted on");
    private List<HistoryRow> historyRows = List.of();

    /** Dependencies for alert history and related product/stock data. */
    public LowStockAlertHistoryView(
            LowStockAlertService alertService, ProductService productService, StockService stockService) {
        this.alertService = alertService;
        this.productService = productService;
        this.stockService = stockService;
        setSpacing(true);
        setPadding(true);
        alertedOnField.setClearButtonVisible(true);
        alertedOnField.addValueChangeListener(event -> applyDateFilter(event.getValue()));
        VerticalLayout content = new VerticalLayout(
                new H2("Alert History"), new Anchor("/alerts", "Active alerts"), alertedOnField, grid);
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
        grid.addColumn(row -> row.alert().getAlertedAt()).setHeader("Alerted").setSortable(true);
        grid.addColumn(row -> row.alert().isAcknowledged() ? "Reviewed" : "Not reviewed")
                .setHeader("Review status");
        grid.addColumn(row -> row.alert().getResolvedAt() == null ? "Active" : "Replenished")
                .setHeader("Stock status");
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                Long alertId = Objects.requireNonNull(event.getValue().alert().getId());
                UI.getCurrent().navigate("alerts/" + alertId);
            }
        });
    }

    private void refresh() {
        List<LowStockAlert> alerts = alertService.history();
        List<Long> productIds =
                alerts.stream().map(LowStockAlert::getProductId).distinct().toList();
        Map<Long, Product> products = productService.findByIds(productIds);
        Map<Long, Stock> stocks = stockService.findByProductIds(productIds);
        historyRows = alerts.stream()
                .map(alert -> new HistoryRow(
                        alert,
                        Objects.requireNonNull(products.get(alert.getProductId())),
                        Objects.requireNonNull(stocks.get(alert.getProductId()))))
                .toList();
        applyDateFilter(alertedOnField.getValue());
    }

    /** Filters loaded episodes by alert date in the system time zone; null restores all loaded episodes. */
    private void applyDateFilter(@Nullable LocalDate filterDate) {
        if (filterDate == null) {
            grid.setItems(historyRows);
            return;
        }
        grid.setItems(historyRows.stream()
                .filter(row -> row.alert()
                        .getAlertedAt()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .equals(filterDate))
                .toList());
    }

    private record HistoryRow(LowStockAlert alert, Product product, Stock stock) {}
}
