package com.example.retailstore.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /**
     * Returns the creation time recorded by JPA auditing.
     *
     * @return the creation time, or {@code null} before auditing populates it
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the last modification time recorded by JPA auditing.
     *
     * @return the last modification time, or {@code null} before auditing populates it
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns the version JPA uses for optimistic locking.
     *
     * @return the current optimistic-lock version
     */
    public long getVersion() {
        return version;
    }
}
