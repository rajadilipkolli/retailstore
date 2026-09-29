package com.example.retailstore.alerts;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence queries for current and historical low-stock alerts. */
public interface LowStockAlertRepository extends JpaRepository<LowStockAlert, Long> {

    /** Finds the active low-stock episode for a product, if one exists. */
    Optional<LowStockAlert> findFirstByProductIdAndResolvedAtIsNullOrderByAlertedAtDescIdDesc(Long productId);

    /** Lists alerts requiring review, newest first. */
    List<LowStockAlert> findAllByAcknowledgedFalseAndResolvedAtIsNullOrderByAlertedAtDescIdDesc();

    /** Lists all alert episodes, newest first. */
    List<LowStockAlert> findAllByOrderByAlertedAtDescIdDesc();
}
