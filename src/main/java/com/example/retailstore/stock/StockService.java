package com.example.retailstore.stock;

import com.example.retailstore.catalog.ProductCatalogCleared;
import com.example.retailstore.catalog.ProductCreated;
import com.example.retailstore.catalog.ProductReorderLevelChanged;
import com.example.retailstore.shared.events.SpringEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads stock balances and records authorized adjustments atomically. */
@Service
public class StockService {

    private final StockRepository stockRepository;
    private final StockHistoryRepository historyRepository;
    private final Clock clock;
    private final SpringEventPublisher eventPublisher;

    /** Dependencies for current and historical inventory records. */
    public StockService(
            StockRepository stockRepository,
            StockHistoryRepository historyRepository,
            Clock clock,
            SpringEventPublisher eventPublisher) {
        this.stockRepository = stockRepository;
        this.historyRepository = historyRepository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    /** @return all balances ordered by product identifier */
    @Transactional(readOnly = true)
    public List<Stock> listAll() {
        return stockRepository.findAllByOrderByProductIdAsc();
    }

    /** Loads a product's current balance. */
    @Transactional(readOnly = true)
    public Stock findByProductId(Long productId) {
        return stockRepository
                .findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found for product: " + productId));
    }

    /** @return the immutable adjustment history, newest first */
    @Transactional(readOnly = true)
    public List<StockHistory> history(Long productId) {
        return historyRepository.findAllByProductIdOrderByTimestampDescIdDesc(productId);
    }

    /** Creates the initial balance after the catalog persists a product. */
    @EventListener
    @Transactional
    public void productCreated(ProductCreated event) {
        initialize(event.productId(), event.initialStock());
    }

    /** Re-evaluates the existing balance when the catalog changes its reorder threshold. */
    @EventListener
    @Transactional
    public void productReorderLevelChanged(ProductReorderLevelChanged event) {
        stockRepository
                .findByProductId(event.productId())
                .ifPresent(stock -> eventPublisher.publish(
                        new StockLevelChanged(event.productId(), stock.getQuantityOnHand(), clock.instant())));
    }

    /** Removes dependent stock data before the catalog deletes its products. */
    @EventListener
    @Transactional
    public void productCatalogCleared(ProductCatalogCleared event) {
        deleteAll();
    }

    /**
     * Applies one adjustment and appends its history record within the same transaction.
     *
     * @throws IllegalArgumentException if the adjustment is invalid or would make stock negative
     */
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Stock adjust(Long productId, StockChangeType changeType, int quantity, String reason) {
        if (changeType == null) {
            throw new IllegalArgumentException("Change type is required.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Adjustment quantity must be a positive whole number.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason or reference is required.");
        }
        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 500) {
            throw new IllegalArgumentException("Reason must be at most 500 characters.");
        }

        Stock stock = stockRepository
                .findByProductIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found for product: " + productId));
        long updatedQuantity = changeType == StockChangeType.IN
                ? (long) stock.getQuantityOnHand() + quantity
                : (long) stock.getQuantityOnHand() - quantity;
        if (updatedQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative.");
        }
        if (updatedQuantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Stock quantity exceeds the supported limit.");
        }

        Instant now = clock.instant();
        stock.update((int) updatedQuantity, now);
        stockRepository.save(stock);
        historyRepository.save(new StockHistory(productId, quantity, changeType, normalizedReason, now));
        eventPublisher.publish(new StockLevelChanged(productId, (int) updatedQuantity, now));
        return stock;
    }

    /** Creates an initial balance once, leaving existing balances untouched. */
    @Transactional
    public void initialize(Long productId, int initialQuantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Product must be persisted before stock is initialized.");
        }
        if (stockRepository.findByProductId(productId).isEmpty()) {
            Instant now = clock.instant();
            stockRepository.save(new Stock(productId, initialQuantity, now));
            eventPublisher.publish(new StockLevelChanged(productId, initialQuantity, now));
        }
    }

    /** Removes stock records when catalog integration tests reset their products. */
    @Transactional
    public void deleteAll() {
        historyRepository.deleteAllInBatch();
        stockRepository.deleteAllInBatch();
    }
}
