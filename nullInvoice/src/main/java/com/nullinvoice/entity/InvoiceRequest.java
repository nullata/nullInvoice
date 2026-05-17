// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "invoice_requests")
@Data
public class InvoiceRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "status", nullable = false, length = 32)
    private String status = STATUS_PENDING;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "request_payload", nullable = false, columnDefinition = "longtext")
    private String requestPayload;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = STATUS_PENDING;
        }
        if (attempts == null) {
            attempts = 0;
        }
        if (maxAttempts == null) {
            maxAttempts = 3;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
