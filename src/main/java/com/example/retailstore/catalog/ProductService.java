package com.example.retailstore.catalog;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> listAll() {
        return productRepository.findAll(Sort.by(Sort.Order.asc("sku")));
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

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

    @Transactional
    public void deleteAll() {
        productRepository.deleteAll();
    }

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

    private String normalizeSku(String sku) {
        return sku.trim();
    }
}
