package com.example.retailstore.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence queries for catalog products. */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Finds a product by its stock keeping unit.
     *
     * @param sku stock keeping unit to search for
     * @return the matching product, if present
     */
    Optional<Product> findBySku(String sku);

    /**
     * Checks whether a stock keeping unit is already assigned.
     *
     * @param sku stock keeping unit to check
     * @return whether a product uses the SKU
     */
    boolean existsBySku(String sku);

    /**
     * Checks whether another product uses a stock keeping unit.
     *
     * @param sku stock keeping unit to check
     * @param id identifier of the product to exclude
     * @return whether a different product uses the SKU
     */
    boolean existsBySkuAndIdNot(String sku, Long id);
}
