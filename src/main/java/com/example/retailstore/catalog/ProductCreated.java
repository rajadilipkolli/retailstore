package com.example.retailstore.catalog;

import com.example.retailstore.shared.events.DomainEvent;

/** Published after a product is persisted so dependent modules can initialize their records. */
public record ProductCreated(Long productId, int initialStock) implements DomainEvent {}
