package com.example.retailstore.usecases.uc004_low_stock_alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.retailstore.alerts.LowStockAlert;
import com.example.retailstore.alerts.LowStockAlertDetailView;
import com.example.retailstore.alerts.LowStockAlertHistoryView;
import com.example.retailstore.alerts.LowStockAlertListView;
import com.example.retailstore.alerts.LowStockAlertService;
import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.stock.StockChangeType;
import com.example.retailstore.stock.StockLevelChanged;
import com.example.retailstore.stock.StockService;
import com.example.retailstore.suppliers.ProductSupplier;
import com.example.retailstore.suppliers.SupplierService;
import com.example.retailstore.usecases.TestcontainersConfig;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.RouteParameters;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class UC004ManageLowStockAlertsTest extends SpringBrowserlessTest {

    @Autowired
    private LowStockAlertService alertService;

    @Autowired
    private StockService stockService;

    @Autowired
    private ProductService productService;

    @Autowired
    private SupplierService supplierService;

    @BeforeEach
    void clearData() {
        alertService.deleteAll();
        supplierService.deleteAll();
        productService.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void mainFlow_generatesAlertAndShowsSupplierOptions() {
        Product product = createProduct("SKU-401", "Torque Wrench", 4, 2);
        Long productId = Objects.requireNonNull(product.getId());
        Long slowerSupplier = Objects.requireNonNull(supplierService
                .create("Long Lead Supply", "", "long@test.test", "", "", "")
                .getId());
        Long fasterSupplier = Objects.requireNonNull(supplierService
                .create("Quick Parts", "", "quick@test.test", "", "", "")
                .getId());
        supplierService.associateProduct(slowerSupplier, productId, "LL-TW", 12);
        supplierService.associateProduct(fasterSupplier, productId, "QP-TW", 3);

        stockService.adjust(productId, StockChangeType.OUT, 2, "Customer Order SO-401");

        LowStockAlert alert = alertService.activeAlerts().getFirst();
        assertThat(alert.getProductId()).isEqualTo(productId);
        assertThat(alert.isAcknowledged()).isFalse();
        assertThat(alertService.supplierOptions(productId))
                .extracting(ProductSupplier::getSupplierSku)
                .containsExactly("QP-TW", "LL-TW");
        UI.getCurrent()
                .navigate(
                        LowStockAlertDetailView.class,
                        new RouteParameters(
                                "alertId", Objects.requireNonNull(alert.getId()).toString()));
        assertThat(find(Grid.class).single().getListDataView().getItemCount()).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br01_alertIsCreatedWhenQuantityReachesReorderLevel() {
        Product product = createProduct("SKU-402", "Hex Key Set", 4, 2);

        stockService.adjust(Objects.requireNonNull(product.getId()), StockChangeType.OUT, 2, "Count correction");

        assertThat(alertService.activeAlerts()).hasSize(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br01_reorderLevelChangesReconcileExistingStock() {
        Product product = createProduct("SKU-415", "Impact Socket", 5, 2);
        Long productId = Objects.requireNonNull(product.getId());

        productService.update(productId, "SKU-415", "Impact Socket", "Tools", "", new BigDecimal("2.50"), 5, 5);

        LowStockAlert alert = alertService.activeAlerts().getFirst();
        assertThat(alert.getProductId()).isEqualTo(productId);

        productService.update(productId, "SKU-415", "Impact Socket", "Tools", "", new BigDecimal("2.50"), 2, 5);

        assertThat(alertService.activeAlerts()).isEmpty();
        assertThat(alertService.findById(Objects.requireNonNull(alert.getId())).isResolved())
                .isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br02_onlyUnacknowledgedAlertsAppearInActiveList() {
        Product first = createProduct("SKU-403", "Cutting Pliers", 4, 2);
        Product second = createProduct("SKU-404", "Bench Clamp", 4, 2);
        stockService.adjust(Objects.requireNonNull(first.getId()), StockChangeType.OUT, 2, "Order SO-403");
        stockService.adjust(Objects.requireNonNull(second.getId()), StockChangeType.OUT, 2, "Order SO-404");
        LowStockAlert firstAlert = alertService.activeAlerts().stream()
                .filter(alert -> alert.getProductId().equals(first.getId()))
                .findFirst()
                .orElseThrow();

        alertService.acknowledge(Objects.requireNonNull(firstAlert.getId()));

        assertThat(alertService.activeAlerts())
                .extracting(LowStockAlert::getProductId)
                .containsExactly(second.getId());
        assertThat(alertService.history()).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br03_replenishmentResolvesEpisodeAndLaterLowStockCreatesAnother() {
        Product product = createProduct("SKU-405", "Precision Saw", 4, 2);
        Long productId = Objects.requireNonNull(product.getId());
        stockService.adjust(productId, StockChangeType.OUT, 2, "Order SO-405");
        LowStockAlert firstEpisode = alertService.activeAlerts().getFirst();

        stockService.adjust(productId, StockChangeType.IN, 1, "Receipt PO-405");

        assertThat(alertService.activeAlerts()).isEmpty();
        assertThat(alertService
                        .findById(Objects.requireNonNull(firstEpisode.getId()))
                        .isResolved())
                .isTrue();

        stockService.adjust(productId, StockChangeType.OUT, 1, "Order SO-406");

        assertThat(alertService.activeAlerts()).hasSize(1);
        assertThat(alertService.activeAlerts().getFirst().getId()).isNotEqualTo(firstEpisode.getId());
        assertThat(alertService.history()).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br04_acknowledgmentDoesNotResolveAndCanBeReopenedWhileStillLow() {
        Product product = createProduct("SKU-407", "Pipe Cutter", 4, 2);
        stockService.adjust(Objects.requireNonNull(product.getId()), StockChangeType.OUT, 2, "Order SO-407");
        LowStockAlert alert = alertService.activeAlerts().getFirst();
        Long alertId = Objects.requireNonNull(alert.getId());

        LowStockAlert reviewed = alertService.acknowledge(alertId);
        assertThat(reviewed.isAcknowledged()).isTrue();
        assertThat(reviewed.isResolved()).isFalse();
        assertThat(alertService.activeAlerts()).isEmpty();

        alertService.reopen(alertId);

        assertThat(alertService.activeAlerts()).extracting(LowStockAlert::getId).containsExactly(alertId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br05_alertHistoryIsNewestFirst() {
        Product first = createProduct("SKU-408", "Locking Pliers", 4, 2);
        Product second = createProduct("SKU-409", "Utility Shears", 4, 2);
        stockService.adjust(Objects.requireNonNull(first.getId()), StockChangeType.OUT, 2, "Order SO-408");
        stockService.adjust(Objects.requireNonNull(second.getId()), StockChangeType.OUT, 2, "Order SO-409");

        assertThat(alertService.history())
                .isSortedAccordingTo(Comparator.comparing(LowStockAlert::getAlertedAt)
                        .thenComparing(alert -> Objects.requireNonNull(alert.getId()))
                        .reversed());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br06_supplierOptionsAreOrderedByShortestLeadTime() {
        Product product = createProduct("SKU-410", "Drill Bit Set", 4, 2);
        Long productId = Objects.requireNonNull(product.getId());
        Long slow = Objects.requireNonNull(supplierService
                .create("Slow Delivery", "", "slow@test.test", "", "", "")
                .getId());
        Long fast = Objects.requireNonNull(supplierService
                .create("Fast Delivery", "", "fast@test.test", "", "", "")
                .getId());
        supplierService.associateProduct(slow, productId, "SD-DB", 8);
        supplierService.associateProduct(fast, productId, "FD-DB", 2);

        assertThat(alertService.supplierOptions(productId))
                .extracting(ProductSupplier::getSupplierSku)
                .containsExactly("FD-DB", "SD-DB");
    }

    @Test
    @WithAnonymousUser
    void acceptance_activeAlertsAreReadableWithoutAuthentication() {
        createProduct("SKU-411", "Socket Driver", 2, 2);
        navigate(LowStockAlertListView.class);

        assertThat(find(Grid.class).single().getListDataView().getItemCount()).isEqualTo(1);
    }

    @Test
    @WithAnonymousUser
    void acceptance_alertAcknowledgmentRequiresAdministrator() {
        Product product = createProduct("SKU-412", "Wire Stripper", 2, 2);
        Long alertId =
                Objects.requireNonNull(alertService.activeAlerts().getFirst().getId());

        assertThatThrownBy(() -> alertService.acknowledge(alertId)).isInstanceOf(AccessDeniedException.class);
        UI.getCurrent().navigate(LowStockAlertDetailView.class, new RouteParameters("alertId", alertId.toString()));
        assertThat(find(Grid.class).single().getListDataView().getItemCount()).isZero();
        assertThat(product.getName()).isEqualTo("Wire Stripper");
    }

    @Test
    @WithAnonymousUser
    void mainFlow_historyCanBeFilteredByAlertDate() {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);
        Product olderProduct = createProduct("SKU-413", "Angle Grinder", 4, 2);
        Product newerProduct = createProduct("SKU-414", "Impact Driver", 4, 2);
        alertService.stockLevelChanged(new StockLevelChanged(
                Objects.requireNonNull(olderProduct.getId()),
                2,
                yesterday.atStartOfDay(zone).toInstant()));
        alertService.stockLevelChanged(new StockLevelChanged(
                Objects.requireNonNull(newerProduct.getId()),
                2,
                today.atStartOfDay(zone).toInstant()));
        navigate(LowStockAlertHistoryView.class);

        DatePicker dateFilter = find(DatePicker.class).single();
        Grid<?> historyGrid = find(Grid.class).single();
        assertThat(historyGrid.getListDataView().getItemCount()).isEqualTo(2);
        dateFilter.setValue(yesterday);
        assertThat(historyGrid.getListDataView().getItemCount()).isEqualTo(1);
        dateFilter.clear();
        assertThat(historyGrid.getListDataView().getItemCount()).isEqualTo(2);
    }

    private Product createProduct(String sku, String name, int initialStock, int reorderLevel) {
        return productService.save(
                sku, name, "Tools", "Inventory item", new BigDecimal("2.50"), reorderLevel, initialStock);
    }
}
