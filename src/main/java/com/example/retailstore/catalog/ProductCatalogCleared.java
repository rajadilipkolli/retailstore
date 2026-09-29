package com.example.retailstore.catalog;

import com.example.retailstore.shared.events.DomainEvent;

/** Published before catalog products are deleted so dependent modules can remove their references. */
public record ProductCatalogCleared() implements DomainEvent {}
