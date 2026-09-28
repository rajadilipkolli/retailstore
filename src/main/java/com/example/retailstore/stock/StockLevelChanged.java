package com.example.retailstore.stock;

import com.example.retailstore.shared.events.DomainEvent;
import java.time.Instant;

/** Published in the stock transaction whenever a product's current level is established or changed. */
public record StockLevelChanged(Long productId, int quantityOnHand, Instant occurredAt) implements DomainEvent {}
