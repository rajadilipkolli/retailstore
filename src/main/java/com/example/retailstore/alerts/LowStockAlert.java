package com.example.retailstore.alerts;

import com.example.retailstore.shared.persistence.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** A persisted low-stock alert that remains visible in history after replenishment. */
@Entity
@Table(name = "low_stock_alerts")
public class LowStockAlert extends BaseEntity {

    @Id
    @Tsid
    private @Nullable Long id;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "alerted_at", nullable = false, updatable = false)
    private Instant alertedAt;

    @Column(nullable = false)
    private boolean acknowledged;

    @Column(name = "resolved_at")
    private @Nullable Instant resolvedAt;

    protected LowStockAlert() {}

    /** Creates an alert on the transition into low stock. */
    public LowStockAlert(Long productId, Instant alertedAt) {
        this.productId = productId;
        this.alertedAt = alertedAt;
        this.acknowledged = false;
    }

    /** @return the generated alert identifier */
    public @Nullable Long getId() {
        return id;
    }

    /** @return the affected product identifier */
    public Long getProductId() {
        return productId;
    }

    /** @return when this low-stock episode began */
    public Instant getAlertedAt() {
        return alertedAt;
    }

    /** @return whether an administrator reviewed this alert */
    public boolean isAcknowledged() {
        return acknowledged;
    }

    /** @return when stock was replenished above reorder level, if resolved */
    public @Nullable Instant getResolvedAt() {
        return resolvedAt;
    }

    /** @return whether stock has been replenished above reorder level */
    public boolean isResolved() {
        return resolvedAt != null;
    }

    /** Marks this alert reviewed without resolving the low-stock condition. */
    void acknowledge() {
        acknowledged = true;
    }

    /** Reopens a reviewed alert while the low-stock condition remains active. */
    void reopen() {
        acknowledged = false;
    }

    /** Resolves this low-stock episode after stock rises above reorder level. */
    void resolve(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
