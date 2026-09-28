package com.example.retailstore.stock;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.shared.ui.NavigationPanel;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Public stock listing and detail view with adjustment history. */
@Route("stock")
@AnonymousAllowed
public class StockTrackingView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ProductService productService;
    private final StockService stockService;
    private final Grid<StockRow> grid = new Grid<>(StockRow.class, false);
    private final Grid<StockHistory> historyGrid = new Grid<>(StockHistory.class, false);
    private final TextField searchField = new TextField("Search SKU or product name");
    private final VerticalLayout detail = new VerticalLayout();
    private List<StockRow> rows = List.of();

    /** Dependencies for public stock information. */
    public StockTrackingView(ProductService productService, StockService stockService) {
        this.productService = productService;
        this.stockService = stockService;
        setSpacing(true);
        setPadding(true);
        VerticalLayout content = content();
        HorizontalLayout layout = new HorizontalLayout(new NavigationPanel(), content);
        layout.addClassName("stock-layout");
        layout.setWidthFull();
        layout.setFlexGrow(1, content);
        add(layout);
        configureGrid();
        configureHistoryGrid();
        refreshRows();
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(event -> filterRows(event.getValue()));
    }

    /** Builds the stock list and selected product detail. */
    private VerticalLayout content() {
        detail.setVisible(false);
        detail.addClassName("stock-detail");
        VerticalLayout content = new VerticalLayout(new H2("Stock Tracking"), searchField, grid, detail);
        content.setWidthFull();
        return content;
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long productId) {
        if (productId == null) {
            return;
        }
        Product product;
        try {
            product = productService.findById(productId);
        } catch (IllegalArgumentException exception) {
            event.rerouteToError(NotFoundException.class);
            return;
        }
        showDetails(product);
    }

    private void configureGrid() {
        grid.addColumn(row -> row.product().getSku()).setHeader("SKU").setSortable(true);
        grid.addColumn(row -> row.product().getName()).setHeader("Product").setSortable(true);
        grid.addColumn(row -> row.stock().getQuantityOnHand())
                .setHeader("Quantity on hand")
                .setSortable(true);
        grid.addColumn(row -> row.product().getReorderLevel())
                .setHeader("Reorder level")
                .setSortable(true);
        grid.addColumn(row -> row.stock().getLastUpdated())
                .setHeader("Last updated")
                .setSortable(true);
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                Long id = Objects.requireNonNull(event.getValue().product().getId());
                UI.getCurrent().navigate("stock/" + id);
            }
        });
    }

    private void configureHistoryGrid() {
        historyGrid.addColumn(StockHistory::getTimestamp).setHeader("Changed");
        historyGrid.addColumn(StockHistory::getChangeType).setHeader("Type");
        historyGrid.addColumn(StockHistory::getQuantity).setHeader("Quantity");
        historyGrid.addColumn(StockHistory::getReason).setHeader("Reason");
    }

    private void refreshRows() {
        Map<Long, Stock> balances =
                stockService.listAll().stream().collect(Collectors.toMap(Stock::getProductId, Function.identity()));
        rows = productService.listAll().stream()
                .map(product -> {
                    Long id = Objects.requireNonNull(product.getId());
                    Stock stock = balances.get(id);
                    if (stock == null) {
                        stockService.initialize(id, product.getInitialStock());
                        stock = stockService.findByProductId(id);
                    }
                    return new StockRow(product, stock);
                })
                .toList();
        grid.setItems(rows);
    }

    private void filterRows(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        grid.setItems(rows.stream()
                .filter(row -> row.product().getSku().toLowerCase(Locale.ROOT).contains(normalized)
                        || row.product().getName().toLowerCase(Locale.ROOT).contains(normalized))
                .toList());
    }

    private void showDetails(Product product) {
        Long id = Objects.requireNonNull(product.getId());
        Stock stock = stockService.findByProductId(id);
        detail.removeAll();
        detail.add(
                new H3(product.getName()),
                new Paragraph("SKU: " + product.getSku()),
                new Paragraph("Quantity on hand: " + stock.getQuantityOnHand()),
                new Paragraph("Reorder level: " + product.getReorderLevel()),
                new Paragraph("Last updated: " + stock.getLastUpdated()));
        if (isAdmin()) {
            detail.add(new Anchor("/stock/" + id + "/adjust", "Record stock change"));
        }
        detail.add(new H3("Stock history"), historyGrid);
        historyGrid.setItems(stockService.history(id));
        detail.setVisible(true);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private record StockRow(Product product, Stock stock) {}
}
