// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.repository;

import com.nullinvoice.entity.InvoiceRequest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRequestRepository extends JpaRepository<InvoiceRequest, Long> {

    @Query("SELECT r FROM InvoiceRequest r WHERE r.status = 'PENDING' AND r.attempts < r.maxAttempts ORDER BY r.createdAt ASC")
    List<InvoiceRequest> findPendingForProcessing(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM InvoiceRequest r WHERE r.id = :id")
    Optional<InvoiceRequest> findByIdForUpdate(@Param("id") Long id);
}
