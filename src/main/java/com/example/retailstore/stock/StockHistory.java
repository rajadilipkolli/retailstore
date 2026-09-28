package com.example.retailstore.stock;

import com.example.retailstore.shared.persistence.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** An immutable audit entry for a stock adjustment. */
@Entity
@Table(name = "stock_history")
public class StockHistory extends BaseEntity {

    @Id
    @Tsid
    private @Nullable Long id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 3, updatable = false)
    private StockChangeType changeType;

    @Column(nullable = false, length = 500, updatable = false)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    protected StockHistory() {}

    /** Records a stock adjustment. */
    public StockHistory(Long productId, int quantity, StockChangeType changeType, String reason, Instant timestamp) {
        this.productId = productId;
        this.quantity = quantity;
        this.changeType = changeType;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    /** @return the associated product identifier */
    public Long getProductId() {
        return productId;
    }

    /** @return the quantity moved */
    public int getQuantity() {
        return quantity;
    }

    /** @return whether stock was received or removed */
    public StockChangeType getChangeType() {
        return changeType;
    }

    /** @return the required adjustment reason */
    public String getReason() {
        return reason;
    }

    /** @return when the adjustment was recorded */
    public Instant getTimestamp() {
        return timestamp;
    }
}
