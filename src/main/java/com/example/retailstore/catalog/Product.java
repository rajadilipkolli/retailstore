package com.example.retailstore.catalog;

import com.example.retailstore.shared.persistence.BaseEntity;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** A catalog item with its initial stock and reorder threshold. */
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Id
    @Tsid
    private @Nullable Long id;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "unit_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel;

    @Column(name = "initial_stock", nullable = false)
    private int initialStock;

    /** Creates an instance for JPA. */
    protected Product() {}

    /**
     * Creates a product with the values supplied by the catalog form.
     *
     * @param sku unique stock keeping unit
     * @param name display name
     * @param category product category
     * @param description optional details
     * @param unitCost cost per unit
     * @param reorderLevel stock level that triggers reordering
     * @param initialStock stock quantity at creation
     */
    public Product(
            String sku,
            String name,
            String category,
            String description,
            BigDecimal unitCost,
            int reorderLevel,
            int initialStock) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.description = description;
        this.unitCost = unitCost;
        this.reorderLevel = reorderLevel;
        this.initialStock = initialStock;
    }

    /** @return the generated identifier, or {@code null} before persistence */
    public @Nullable Long getId() {
        return id;
    }

    /** @return the unique stock keeping unit */
    public String getSku() {
        return sku;
    }

    /** @param sku the stock keeping unit */
    public void setSku(String sku) {
        this.sku = sku;
    }

    /** @return the display name */
    public String getName() {
        return name;
    }

    /** @param name the display name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the product category */
    public String getCategory() {
        return category;
    }

    /** @param category the product category */
    public void setCategory(String category) {
        this.category = category;
    }

    /** @return the product details */
    public String getDescription() {
        return description;
    }

    /** @param description the product details */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the cost per unit */
    public BigDecimal getUnitCost() {
        return unitCost;
    }

    /** @param unitCost the cost per unit */
    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    /** @return the stock level that triggers reordering */
    public int getReorderLevel() {
        return reorderLevel;
    }

    /** @param reorderLevel the stock level that triggers reordering */
    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    /** @return the stock quantity at creation */
    public int getInitialStock() {
        return initialStock;
    }

    /** @param initialStock the stock quantity at creation */
    public void setInitialStock(int initialStock) {
        this.initialStock = initialStock;
    }
}
