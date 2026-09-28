package com.example.retailstore.stock;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence queries for the append-only stock adjustment history. */
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    /** Lists a product's history from newest to oldest. */
    List<StockHistory> findAllByProductIdOrderByTimestampDescIdDesc(Long productId);
}
