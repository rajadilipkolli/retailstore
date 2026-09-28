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

    /** Reconciles an alert in the transaction that changed the stock balance. */
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

    /** @return all alert episodes, including acknowledged and resolved entries */
    @Transactional(readOnly = true)
    public List<LowStockAlert> history() {
        return alertRepository.findAllByOrderByAlertedAtDescIdDesc();
    }

    /** @return supplier offers ordered by shortest lead time, then supplier identifier */
    @Transactional(readOnly = true)
    public List<ProductSupplier> supplierOptions(Long productId) {
        return supplierService.suppliersForProduct(productId).stream()
                .sorted(Comparator.comparingInt(ProductSupplier::getLeadTimeDays)
                        .thenComparing(ProductSupplier::getSupplierId))
                .toList();
    }

    /** Loads one alert or reports a domain-level not-found error. */
    @Transactional(readOnly = true)
    public LowStockAlert findById(Long alertId) {
        return alertRepository
                .findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
    }

    /** Marks an active alert reviewed without resolving it. */
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

    /** Reopens an acknowledged alert only while its product remains below or at reorder level. */
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
