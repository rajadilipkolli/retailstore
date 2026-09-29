package com.example.retailstore.stock;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence queries for current stock balances. */
public interface StockRepository extends JpaRepository<Stock, Long> {

    /** Finds the current stock for a product. */
    Optional<Stock> findByProductId(Long productId);

    /** Finds balances for only the requested products. */
    List<Stock> findAllByProductIdIn(Collection<Long> productIds);

    /** Lists balances in product identifier order. */
    List<Stock> findAllByOrderByProductIdAsc();

    /** Finds the current balance while serializing concurrent adjustments. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from Stock stock where stock.productId = :productId")
    Optional<Stock> findByProductIdForUpdate(@Param("productId") Long productId);
}
