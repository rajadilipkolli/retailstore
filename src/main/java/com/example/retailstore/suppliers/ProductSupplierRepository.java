package com.example.retailstore.suppliers;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence queries for supplier product offers. */
public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, Long> {

    /** Lists products associated with a supplier. */
    List<ProductSupplier> findAllBySupplierIdOrderByProductIdAsc(Long supplierId);

    /** Lists suppliers associated with a product. */
    List<ProductSupplier> findAllByProductIdOrderBySupplierIdAsc(Long productId);

    /** Finds an existing supplier-product pair so it can be updated rather than duplicated. */
    Optional<ProductSupplier> findBySupplierIdAndProductId(Long supplierId, Long productId);

    /** Removes one supplier-product pair. */
    void deleteBySupplierIdAndProductId(Long supplierId, Long productId);
}
