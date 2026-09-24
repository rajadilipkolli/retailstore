package com.example.retailstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ProductCatalogViewTest {

    private final ProductService service = mock(ProductService.class);
    private final Product product =
            new Product("SKU-1", "Widget", "Tools", "Steel widget", new BigDecimal("12.50"), 5, 10);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void visitorSeesLoadedProductDetailsWithoutAnEditForm() {
        when(service.findById(1L)).thenReturn(product);
        ProductCatalogView view = new ProductCatalogView(service);

        view.setParameter(mock(BeforeEvent.class), 1L);

        assertThat(descendants(view)
                        .filter(Paragraph.class::isInstance)
                        .map(component -> ((Paragraph) component).getText()))
                .contains("SKU: SKU-1", "Category: Tools", "Description: Steel widget", "Unit cost: 12.50");
        assertThat(descendants(view).filter(BigDecimalField.class::isInstance)).isEmpty();
    }

    @Test
    void missingProductReroutesToNotFoundBeforeShowingDetails() {
        when(service.findById(99L)).thenThrow(new IllegalArgumentException("Product not found"));
        ProductCatalogView view = new ProductCatalogView(service);
        BeforeEvent event = mock(BeforeEvent.class);

        view.setParameter(event, 99L);

        verify(event).rerouteToError(NotFoundException.class);
        assertThat(descendants(view)
                        .filter(component -> component.getClassNames().contains("catalog-details"))
                        .findFirst()
                        .orElseThrow()
                        .isVisible())
                .isFalse();
    }

    @Test
    void administratorKeepsDecimalPrecisionAndRejectsInvalidStockNumbers() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", "unused", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(service.findById(1L)).thenReturn(product);
        ProductCatalogView view = new ProductCatalogView(service);
        view.setParameter(mock(BeforeEvent.class), 1L);

        BigDecimalField cost = descendants(view)
                .filter(BigDecimalField.class::isInstance)
                .map(BigDecimalField.class::cast)
                .findFirst()
                .orElseThrow();
        List<NumberField> stockFields = descendants(view)
                .filter(NumberField.class::isInstance)
                .map(NumberField.class::cast)
                .toList();
        NumberField reorder = stockFields.get(0);
        NumberField initialStock = stockFields.get(1);

        cost.setValue(new BigDecimal("99999999999999999.99"));
        assertThat(product.getUnitCost()).isEqualTo(new BigDecimal("99999999999999999.99"));
        reorder.setValue(2.5);
        initialStock.setValue(2147483648d);
        assertThat(product.getReorderLevel()).isEqualTo(5);
        assertThat(product.getInitialStock()).isEqualTo(10);
        clearInvocations(service);
        descendants(view)
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> "Save product".equals(button.getText()))
                .findFirst()
                .orElseThrow()
                .click();
        verifyNoInteractions(service);
        reorder.setValue(6d);
        initialStock.setValue(11d);
        assertThat(product.getReorderLevel()).isEqualTo(6);
        assertThat(product.getInitialStock()).isEqualTo(11);
    }

    private Stream<Component> descendants(Component component) {
        return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::descendants));
    }
}
