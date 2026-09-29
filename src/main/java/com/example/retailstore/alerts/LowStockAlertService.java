package com.example.retailstore.alerts;

import com.example.retailstore.catalog.Product;
import com.example.retailstore.catalog.ProductCatalogCleared;
import com.example.retailstore.catalog.ProductService;
import com.example.retailstore.stock.Stock;
import com.example.retailstore.stock.StockLevelChanged;
import com.example.retailstore.stock.StockService;
import com.example.retailstore.suppliers.ProductSupplier;
import com.example.retailstore.suppliers.SupplierService;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maintains low-stock alert episodes as stock levels cross reorder thresholds. */
@Service
public class LowStockAlertService {

    private final LowStockAlertRepository alertRepository;
    private final ProductService productService;
    private final StockService stockService;
    private final SupplierService supplierService;

    /** Dependencies for alert persistence and product/stock validation. */
    public LowStockAlertService(
            LowStockAlertRepository alertRepository,
            ProductService productService,
            StockService stockService,
            SupplierService supplierService) {
        this.alertRepository = alertRepository;
        this.productService = productService;
        this.stockService = stockService;
        this.supplierService = supplierService;
    }

    /**
     * Creates an alert at or below the product's current reorder level if no unresolved episode exists; otherwise
     * preserves that episode, including its acknowledgment. Above the threshold, resolves the latest unresolved episode
     * using the event time. Runs in the stock change transaction.
     *
     * @throws IllegalArgumentException if the event's product does not exist
     */
    @EventListener
    @Transactional
    public void stockLevelChanged(StockLevelChanged event) {
        Product product = productService.findById(event.productId());
        var activeAlert =
                alertRepository.findFirstByProductIdAndResolvedAtIsNullOrderByAlertedAtDescIdDesc(event.productId());
        if (event.quantityOnHand() <= product.getReorderLevel()) {
            if (activeAlert.isEmpty()) {
                alertRepository.save(new LowStockAlert(event.productId(), event.occurredAt()));
            }
        } else {
            activeAlert.ifPresent(alert -> {
                alert.resolve(event.occurredAt());
                alertRepository.save(alert);
            });
        }
    }

    /** Removes alerts that reference products before the catalog is cleared. */
    @EventListener
    @Transactional
    public void productCatalogCleared(ProductCatalogCleared event) {
        deleteAll();
    }

    /** @return active unacknowledged alerts in newest-first order */
    @Transactional(readOnly = true)
    public List<LowStockAlert> activeAlerts() {
        return alertRepository.findAllByAcknowledgedFalseAndResolvedAtIsNullOrderByAlertedAtDescIdDesc();
    }

    /** @return all alert episodes, including acknowledged and resolved entries, ordered by alert time and ID descending */
    @Transactional(readOnly = true)
    public List<LowStockAlert> history() {
        return alertRepository.findAllByOrderByAlertedAtDescIdDesc();
    }

    /**
     * @return supplier offers ordered by shortest lead time, then supplier identifier; empty if no offers exist
     * @throws IllegalArgumentException if the product does not exist
     */
    @Transactional(readOnly = true)
    public List<ProductSupplier> supplierOptions(Long productId) {
        return supplierService.suppliersForProduct(productId).stream()
                .sorted(Comparator.comparingInt(ProductSupplier::getLeadTimeDays)
                        .thenComparing(ProductSupplier::getSupplierId))
                .toList();
    }

    /**
     * Loads one alert.
     *
     * @throws IllegalArgumentException if the alert does not exist
     */
    @Transactional(readOnly = true)
    public LowStockAlert findById(Long alertId) {
        return alertRepository
                .findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
    }

    /**
     * Marks an unresolved alert reviewed without resolving it, including an alert already reviewed.
     * Requires the ADMIN role.
     *
     * @return the saved alert
     * @throws IllegalArgumentException if the alert is missing or resolved
     * @throws org.springframework.security.access.AccessDeniedException if the caller lacks the ADMIN role
     * @throws org.springframework.dao.OptimisticLockingFailureException if a concurrent change prevents saving the alert
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public LowStockAlert acknowledge(Long alertId) {
        LowStockAlert alert = findById(alertId);
        if (alert.isResolved()) {
            throw new IllegalArgumentException("Resolved alerts cannot be acknowledged.");
        }
        alert.acknowledge();
        return alertRepository.save(alert);
    }

    /**
     * Clears acknowledgment of an unresolved alert while its product remains at or below reorder level.
     * An already unacknowledged alert is also accepted. Requires the ADMIN role.
     *
     * @return the saved alert
     * @throws IllegalArgumentException if the alert, product, or stock record is missing, the alert is resolved,
     *     or current stock exceeds the reorder level
     * @throws org.springframework.security.access.AccessDeniedException if the caller lacks the ADMIN role
     * @throws org.springframework.dao.OptimisticLockingFailureException if a concurrent change prevents saving the alert
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public LowStockAlert reopen(Long alertId) {
        LowStockAlert alert = findById(alertId);
        if (alert.isResolved()) {
            throw new IllegalArgumentException("Replenished alerts cannot be reopened.");
        }
        Product product = productService.findById(alert.getProductId());
        Stock stock = stockService.findByProductId(alert.getProductId());
        if (stock.getQuantityOnHand() > product.getReorderLevel()) {
            throw new IllegalArgumentException("Alert cannot be reopened after stock is replenished.");
        }
        alert.reopen();
        return alertRepository.save(alert);
    }

    /** Clears alert rows when integration tests reset their product data. */
    @Transactional
    public void deleteAll() {
        alertRepository.deleteAllInBatch();
    }
}
