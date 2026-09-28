package com.example.retailstore.catalog;

import com.example.retailstore.shared.events.DomainEvent;

/** Published when a product's reorder threshold changes. */
public record ProductReorderLevelChanged(Long productId) implements DomainEvent {}
