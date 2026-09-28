package com.example.retailstore.stock;

import com.example.retailstore.shared.persistence.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** The current quantity and last adjustment time for one product. */
@Entity
@Table(name = "stocks")
public class Stock extends BaseEntity {

    @Id
    @Tsid
    private @Nullable Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    protected Stock() {}

    /** Creates the initial stock record for a product. */
    public Stock(Long productId, int quantityOnHand, Instant lastUpdated) {
        this.productId = productId;
        this.quantityOnHand = quantityOnHand;
        this.lastUpdated = lastUpdated;
    }

    /** @return the generated stock identifier */
    public @Nullable Long getId() {
        return id;
    }

    /** @return the associated product identifier */
    public Long getProductId() {
        return productId;
    }

    /** @return the current quantity */
    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    /** @return the time of the latest stock adjustment */
    public Instant getLastUpdated() {
        return lastUpdated;
    }

    /** Applies a validated new balance and its update time. */
    void update(int quantityOnHand, Instant lastUpdated) {
        this.quantityOnHand = quantityOnHand;
        this.lastUpdated = lastUpdated;
    }
}
