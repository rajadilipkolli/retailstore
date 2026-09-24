package com.example.retailstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ProductServiceTest {

    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository);

    @Test
    void rejectsValuesBeyondMappedColumnLengthsOnCreateAndUpdate() {
        assertThatThrownBy(() -> save("S".repeat(101), "Name", "Category", "Description", "1.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU");
        assertThatThrownBy(() -> save("SKU", "N".repeat(201), "Category", "Description", "1.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> save("SKU", "Name", "C".repeat(101), "Description", "1.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category");
        when(repository.findById(1L))
                .thenReturn(Optional.of(new Product("SKU", "Name", "Category", "", BigDecimal.ONE, 1, 1)));
        assertThatThrownBy(() ->
                        service.update(1L, "SKU", "Name", "Category", "D".repeat(1001), new BigDecimal("1.00"), 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Description");
    }

    @Test
    void acceptsExactDecimalRangeButRejectsFractionalCentsAndOverflow() {
        when(repository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product saved = save("SKU", "Name", "Category", "", "99999999999999999.99");

        assertThat(saved.getUnitCost()).isEqualByComparingTo("99999999999999999.99");
        assertThatThrownBy(() -> save("OTHER", "Name", "Category", "", "1.001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decimal places");
        assertThatThrownBy(() -> save("OTHER", "Name", "Category", "", "100000000000000000.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("range");
    }

    @Test
    void translatesOnlySkuConstraintViolationsForCreateAndUpdate() {
        DataIntegrityViolationException duplicate = violation("uk_products_sku");
        when(repository.saveAndFlush(any(Product.class))).thenThrow(duplicate);

        assertThatThrownBy(() -> save("SKU", "Name", "Category", "", "1.00"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU must be unique");

        when(repository.findById(1L))
                .thenReturn(Optional.of(new Product("OLD", "Name", "Category", "", BigDecimal.ONE, 1, 1)));
        assertThatThrownBy(() -> service.update(1L, "SKU", "Name", "Category", "", new BigDecimal("1.00"), 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SKU must be unique");
        verify(repository).existsBySkuAndIdNot("SKU", 1L);
    }

    @Test
    void propagatesUnrelatedPersistenceFailureUnchanged() {
        DataIntegrityViolationException unrelated = violation("other_constraint");
        when(repository.saveAndFlush(any(Product.class))).thenThrow(unrelated);

        assertThatThrownBy(() -> save("SKU", "Name", "Category", "", "1.00")).isSameAs(unrelated);
    }

    private Product save(String sku, String name, String category, String description, String unitCost) {
        return service.save(sku, name, category, description, new BigDecimal(unitCost), 1, 1);
    }

    private DataIntegrityViolationException violation(String constraintName) {
        return new DataIntegrityViolationException(
                "Persistence failure",
                new ConstraintViolationException("constraint", new SQLException("constraint"), constraintName));
    }
}
