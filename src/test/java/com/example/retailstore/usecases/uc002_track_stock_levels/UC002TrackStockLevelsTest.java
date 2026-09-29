package com.example.retailstore.usecases.uc002_track_stock_levels;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.stock.Stock;
import com.example.retailstore.stock.StockChangeType;
import com.example.retailstore.stock.StockHistory;
import com.example.retailstore.stock.StockService;
import com.example.retailstore.stock.StockTrackingView;
import com.example.retailstore.usecases.TestcontainersConfig;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import java.math.BigDecimal;
import java.util.List;
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
class UC002TrackStockLevelsTest extends SpringBrowserlessTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private StockService stockService;

    @BeforeEach
    void clearProducts() {
        productService.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void mainFlow_recordsIncomingAndOutgoingStockWithHistory() {
        Product product = createProduct("SKU-100", "Widget Alpha");
        Long productId = Objects.requireNonNull(product.getId());
        Stock initial = stockService.findByProductId(productId);

        stockService.adjust(productId, StockChangeType.IN, 5, "Purchase Order PO-123");
        Stock updated = stockService.adjust(productId, StockChangeType.OUT, 2, "Customer Order SO-456");

        assertThat(updated.getQuantityOnHand()).isEqualTo(13);
        assertThat(updated.getLastUpdated()).isAfterOrEqualTo(initial.getLastUpdated());
        assertThat(stockService.history(productId))
                .extracting(StockHistory::getReason)
                .contains("Purchase Order PO-123", "Customer Order SO-456");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br01_outgoingAdjustmentCannotMakeStockNegative() {
        Product product = createProduct("SKU-200", "Widget Beta");
        Long productId = Objects.requireNonNull(product.getId());

        assertThatThrownBy(() -> stockService.adjust(productId, StockChangeType.OUT, 11, "Shipment SO-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");

        assertThat(stockService.findByProductId(productId).getQuantityOnHand()).isEqualTo(10);
        assertThat(stockService.history(productId)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br02_reasonIsRequiredAndTrimmed() {
        Product product = createProduct("SKU-300", "Widget Gamma");
        Long productId = Objects.requireNonNull(product.getId());

        assertThatThrownBy(() -> stockService.adjust(productId, StockChangeType.IN, 1, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");

        stockService.adjust(productId, StockChangeType.IN, 1, "  PO-789  ");
        assertThat(stockService.history(productId).getFirst().getReason()).isEqualTo("PO-789");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br03_historyIsAppendOnlyAcrossAdjustments() {
        Product product = createProduct("SKU-400", "Widget Delta");
        Long productId = Objects.requireNonNull(product.getId());
        stockService.adjust(productId, StockChangeType.OUT, 1, "Order SO-9");
        stockService.adjust(productId, StockChangeType.IN, 4, "Receipt PO-9");

        assertThat(stockService.history(productId))
                .extracting(StockHistory::getReason)
                .containsExactlyInAnyOrder("Order SO-9", "Receipt PO-9");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br04_changeTypeIsEitherIncomingOrOutgoing() {
        Product product = createProduct("SKU-500", "Widget Epsilon");
        Long productId = Objects.requireNonNull(product.getId());
        stockService.adjust(productId, StockChangeType.IN, 3, "Receipt");
        stockService.adjust(productId, StockChangeType.OUT, 2, "Usage");

        assertThat(stockService.history(productId))
                .extracting(StockHistory::getChangeType)
                .containsExactlyInAnyOrder(StockChangeType.IN, StockChangeType.OUT);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void br05_balanceAndLastUpdatedChangeImmediately() {
        Product product = createProduct("SKU-600", "Widget Zeta");
        Long productId = Objects.requireNonNull(product.getId());
        Stock before = stockService.findByProductId(productId);

        Stock after = stockService.adjust(productId, StockChangeType.IN, 2, "Count correction");

        assertThat(stockService.findByProductId(productId).getQuantityOnHand()).isEqualTo(12);
        assertThat(after.getLastUpdated()).isAfterOrEqualTo(before.getLastUpdated());
        assertThat(stockService.history(productId)).hasSize(1);
    }

    @Test
    @WithAnonymousUser
    void br06_adjustmentIsRestrictedToAdministrators() {
        Product product = createProduct("SKU-700", "Widget Eta");

        assertThatThrownBy(() -> stockService.adjust(
                        Objects.requireNonNull(product.getId()), StockChangeType.IN, 1, "Unauthorized"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithAnonymousUser
    void acceptance_searchFindsProductsBySkuAndName() {
        createProduct("SKU-801", "Copper Fastener");
        createProduct("SKU-802", "Steel Bracket");
        navigate(StockTrackingView.class);

        TextField searchField = find(TextField.class).single();
        List<Grid> grids = find(Grid.class).all();
        Grid<?> stockGrid = grids.getFirst();

        assertThat(stockGrid.getListDataView().getItemCount()).isEqualTo(2);
        searchField.setValue("SKU-801");
        assertThat(stockGrid.getListDataView().getItemCount()).isEqualTo(1);
        searchField.setValue("bracket");
        assertThat(stockGrid.getListDataView().getItemCount()).isEqualTo(1);
    }

    private Product createProduct(String sku, String name) {
        return productService.save(sku, name, "Hardware", "Inventory item", new BigDecimal("2.50"), 5, 10);
    }
}
