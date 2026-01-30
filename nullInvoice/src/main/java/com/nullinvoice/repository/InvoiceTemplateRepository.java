// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.repository;

import com.nullinvoice.entity.InvoiceTemplates;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceTemplateRepository extends JpaRepository<InvoiceTemplates, Long> {

    Optional<InvoiceTemplates> findByNameIgnoreCase(String name);

    Optional<InvoiceTemplates> findFirstByIsDefaultTrue();

    List<InvoiceTemplates> findAllByOrderByNameAsc();

    @Modifying
    @Query("update InvoiceTemplates t set t.isDefault = false where t.isDefault = true and t.id <> :keepId")
    void clearDefaultExcept(@Param("keepId") Long keepId);
}
