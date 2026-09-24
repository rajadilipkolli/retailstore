package com.example.retailstore.usecases.uc001_manage_product_catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.usecases.BaseIT;
import java.math.BigDecimal;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UC001ManageProductCatalogTest extends BaseIT {

    @Autowired
    private ProductService productService;

    /** Starts each catalog scenario with no products. */
    @BeforeEach
    void clearProducts() {
        productService.deleteAll();
    }

    /** Verifies that a product can be created and its details edited. */
    @Test
    void mainFlow_createAndEditProduct() {
        Product created =
                productService.save("SKU-100", "Widget", "Tools", "Steel widget", new BigDecimal("12.50"), 5, 10);

        assertThat(productService.listAll()).hasSize(1);
        assertThat(created.getSku()).isEqualTo("SKU-100");
        Long createdId = Objects.requireNonNull(created.getId());

        Product updated = productService.update(
                createdId,
                "SKU-100",
                "Updated Widget",
                "Hardware",
                "Updated description",
                new BigDecimal("13.75"),
                10,
                12);

        assertThat(updated.getName()).isEqualTo("Updated Widget");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(updated.getUnitCost()).isEqualByComparingTo(new BigDecimal("13.75"));
    }

    /** Verifies that two products cannot share a SKU. */
    @Test
    void br01_duplicateSkuIsRejected() {
        productService.save("SKU-200", "Widget", "Tools", "Steel widget", new BigDecimal("12.50"), 5, 10);

        assertThatThrownBy(() -> productService.save(
                        "SKU-200", "Another Widget", "Tools", "Duplicate", new BigDecimal("15.00"), 7, 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU");
    }

    /** Verifies that SKU and name are required when creating a product. */
    @Test
    void br02_requiredFieldsAreValidated() {
        assertThatThrownBy(
                        () -> productService.save("", "Widget", "Tools", "Missing SKU", new BigDecimal("12.50"), 5, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU");

        assertThatThrownBy(() ->
                        productService.save("SKU-300", "", "Tools", "Missing name", new BigDecimal("12.50"), 5, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    /** Verifies that cost and reorder level must be positive. */
    @Test
    void br03_costAndReorderLevelMustBePositive() {
        assertThatThrownBy(() -> productService.save("SKU-400", "Widget", "Tools", "Zero cost", BigDecimal.ZERO, 5, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cost");

        assertThatThrownBy(() -> productService.save(
                        "SKU-401", "Widget", "Tools", "Zero reorder", new BigDecimal("10.00"), 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reorder");
    }

    /** Verifies that the reorder threshold cannot exceed starting stock. */
    @Test
    void br04_reorderLevelCannotExceedInitialStock() {
        assertThatThrownBy(() ->
                        productService.save("SKU-402", "Widget", "Tools", "Bad stock", new BigDecimal("10.00"), 12, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("initial stock");
    }
}
