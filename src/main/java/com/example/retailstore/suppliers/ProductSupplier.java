package com.example.retailstore.suppliers;

import com.example.retailstore.shared.persistence.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/** A supplier's offer for one product. */
@Entity
@Table(name = "product_suppliers")
public class ProductSupplier extends BaseEntity {

    @Id
    @Tsid
    private @Nullable Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "supplier_sku", nullable = false, length = 100)
    private String supplierSku;

    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays;

    protected ProductSupplier() {}

    /** Creates a supplier-to-product offer. */
    public ProductSupplier(Long supplierId, Long productId, String supplierSku, int leadTimeDays) {
        this.supplierId = supplierId;
        this.productId = productId;
        this.supplierSku = supplierSku;
        this.leadTimeDays = leadTimeDays;
    }

    /** @return the generated association identifier */
    public @Nullable Long getId() {
        return id;
    }

    /** @return the supplier identifier */
    public Long getSupplierId() {
        return supplierId;
    }

    /** @return the product identifier */
    public Long getProductId() {
        return productId;
    }

    /** @return the supplier's identifier for the product */
    public String getSupplierSku() {
        return supplierSku;
    }

    /** @return the expected delivery time in days */
    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    /** Updates supplier-specific product details. */
    void updateOffer(String supplierSku, int leadTimeDays) {
        this.supplierSku = supplierSku;
        this.leadTimeDays = leadTimeDays;
    }
}
