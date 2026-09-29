package com.example.retailstore.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.stock.Stock;
import com.example.retailstore.stock.StockService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class LowStockAlertViewTest {
    private final LowStockAlertService alerts = mock(LowStockAlertService.class);
    private final ProductService products = mock(ProductService.class);
    private final StockService stocks = mock(StockService.class);

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void gridsBatchDistinctProductIdsAndKeepAllEpisodes(boolean history) {
        var episodes = List.of(
                new LowStockAlert(2L, Instant.now()),
                new LowStockAlert(1L, Instant.now()),
                new LowStockAlert(2L, Instant.now()));
        if (history) {
            when(alerts.history()).thenReturn(episodes);
        } else {
            when(alerts.activeAlerts()).thenReturn(episodes);
        }
        when(products.findByIds(List.of(2L, 1L))).thenReturn(Map.of(1L, mock(Product.class), 2L, mock(Product.class)));
        when(stocks.findByProductIds(List.of(2L, 1L))).thenReturn(Map.of(1L, mock(Stock.class), 2L, mock(Stock.class)));

        Component view = history
                ? new LowStockAlertHistoryView(alerts, products, stocks)
                : new LowStockAlertListView(alerts, products, stocks);

        Grid<?> grid = descendants(view)
                .filter(Grid.class::isInstance)
                .map(Grid.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(grid.getListDataView().getItemCount()).isEqualTo(3);
        verify(products).findByIds(List.of(2L, 1L));
        verify(stocks).findByProductIds(List.of(2L, 1L));
        verifyNoMoreInteractions(products, stocks);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void actionShowsRetryNotificationForConcurrentChanges(boolean acknowledged) {
        assertActionNotification(
                acknowledged,
                new OptimisticLockingFailureException("concurrent update"),
                "The alert was changed by another action. Please retry.");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void actionStillShowsDomainValidationErrors(boolean acknowledged) {
        assertActionNotification(
                acknowledged, new IllegalArgumentException("Alert is resolved."), "Alert is resolved.");
    }

    private void assertActionNotification(boolean acknowledged, RuntimeException failure, String message) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", "unused", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        LowStockAlert alert = mock(LowStockAlert.class);
        when(alert.getId()).thenReturn(1L);
        when(alert.getProductId()).thenReturn(2L);
        when(alert.isAcknowledged()).thenReturn(acknowledged);
        when(alerts.findById(1L)).thenReturn(alert);
        when(products.findById(2L)).thenReturn(mock(Product.class));
        if (acknowledged) {
            when(alerts.reopen(1L)).thenThrow(failure);
        } else {
            when(alerts.acknowledge(1L)).thenThrow(failure);
        }
        var view = new LowStockAlertActionView(alerts, products);
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getRouteParameters()).thenReturn(new RouteParameters("alertId", "1"));
        view.beforeEnter(event);

        try (var notifications = mockStatic(Notification.class)) {
            descendants(view)
                    .filter(Button.class::isInstance)
                    .map(Button.class::cast)
                    .findFirst()
                    .orElseThrow()
                    .click();

            notifications.verify(() -> Notification.show(message, 4000, Notification.Position.MIDDLE));
        }
    }

    private Stream<Component> descendants(Component component) {
        return Stream.concat(Stream.of(component), component.getChildren().flatMap(this::descendants));
    }
}
