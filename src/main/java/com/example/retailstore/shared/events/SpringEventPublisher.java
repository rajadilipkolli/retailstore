package com.example.retailstore.shared.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/** Publishes typed domain events through Spring's application event system. */
@Service
public class SpringEventPublisher {

    private final ApplicationEventPublisher publisher;

    /** @param publisher Spring's application event dispatcher */
    public SpringEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /** Publishes one event to synchronous application listeners. */
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
