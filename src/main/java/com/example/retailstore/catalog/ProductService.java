package com.example.retailstore.catalog;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validates and persists products in the public catalog. */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    /** @param productRepository storage for catalog products */
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** @return all products ordered by stock keeping unit */
    @Transactional(readOnly = true)
    public List<Product> listAll() {
        return productRepository.findAll(Sort.by(Sort.Order.asc("sku")));
    }

    /**
     * Loads a product by its generated identifier.
     *
     * @param id identifier to look up
     * @return the matching product
     * @throws IllegalArgumentException if the product does not exist
     */
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    /**
     * Validates and creates a product with a unique, trimmed SKU.
     *
     * @param sku stock keeping unit
     * @param name display name
     * @param category product category
     * @param description optional details
     * @param unitCost positive cost per unit
     * @param reorderLevel positive threshold no greater than initial stock
     * @param initialStock nonnegative starting quantity
     * @return the saved product
     * @throws IllegalArgumentException if a value is invalid or the SKU is already in use
     */
    @Transactional
    public Product save(
            String sku,
            String name,
            String category,
            String description,
            BigDecimal unitCost,
            int reorderLevel,
            int initialStock) {
        validateProduct(sku, name, category, unitCost, reorderLevel, initialStock, null);
        String normalizedSku = normalizeSku(sku);
        if (productRepository.existsBySku(normalizedSku)) {
            throw new IllegalArgumentException("SKU must be unique across all products.");
        }
        Product product = new Product(
                normalizedSku,
                name.trim(),
                category.trim(),
                description == null ? "" : description.trim(),
                unitCost,
                reorderLevel,
                initialStock);
        return productRepository.save(product);
    }

    /**
     * Validates and updates an existing product.
     *
     * @param id identifier of the product to update
     * @param sku stock keeping unit
     * @param name display name
     * @param category product category
     * @param description optional details
     * @param unitCost positive cost per unit
     * @param reorderLevel positive threshold no greater than initial stock
     * @param initialStock nonnegative starting quantity
     * @return the saved product
     * @throws IllegalArgumentException if the product is missing or a value is invalid
     */
    @Transactional
    public Product update(
            Long id,
            String sku,
            String name,
            String category,
            String description,
            BigDecimal unitCost,
            int reorderLevel,
            int initialStock) {
        Product product = findById(id);
        validateProduct(sku, name, category, unitCost, reorderLevel, initialStock, id);
        String normalizedSku = normalizeSku(sku);
        product.setSku(normalizedSku);
        product.setName(name.trim());
        product.setCategory(category.trim());
        product.setDescription(description == null ? "" : description.trim());
        product.setUnitCost(unitCost);
        product.setReorderLevel(reorderLevel);
        product.setInitialStock(initialStock);
        return productRepository.save(product);
    }

    /** Removes all products; used to reset the catalog between integration tests. */
    @Transactional
    public void deleteAll() {
        productRepository.deleteAll();
    }

    /** Checks required values, stock limits, and SKU uniqueness for a new or existing product. */
    private void validateProduct(
            String sku,
            String name,
            String category,
            BigDecimal unitCost,
            int reorderLevel,
            int initialStock,
            @Nullable Long id) {

        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (unitCost == null || unitCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit cost must be positive.");
        }
        if (reorderLevel <= 0) {
            throw new IllegalArgumentException("reorder level must be positive.");
        }
        if (initialStock < 0) {
            throw new IllegalArgumentException("Initial stock cannot be negative.");
        }
        if (reorderLevel > initialStock) {
            throw new IllegalArgumentException(
                    "Reorder level must be less than or equal to the initial stock quantity.");
        }

        String normalizedSku = normalizeSku(sku);
        if (id == null) {
            if (productRepository.existsBySku(normalizedSku)) {
                throw new IllegalArgumentException("SKU must be unique across all products.");
            }
        } else if (productRepository.existsBySkuAndIdNot(normalizedSku, id)) {
            throw new IllegalArgumentException("SKU must be unique across all products.");
        }
    }

    /** Trims surrounding whitespace before persisting or comparing a SKU. */
    private String normalizeSku(String sku) {
        return sku.trim();
    }
}
