// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.repository;

import com.nullinvoice.entity.Invoices;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoices, Long> {

    @EntityGraph(attributePaths = {"invoiceItemsCollection", "templateId"})
    List<Invoices> findAll(Sort sort);

    @EntityGraph(attributePaths = {"invoiceItemsCollection", "templateId"})
    Optional<Invoices> findWithItemsByInvoiceNumber(String invoiceNumber);

    @EntityGraph(attributePaths = {"invoiceItemsCollection", "templateId"})
    Optional<Invoices> findWithItemsByInvoiceNumberAndSupplierPartyId_Id(String invoiceNumber, Long supplierId);

    Page<Invoices> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Invoices> findAllByInvoiceNumberContainingIgnoreCase(String invoiceNumber, Pageable pageable);

    Page<Invoices> findAllByClientNameContainingIgnoreCase(String clientName, Pageable pageable);

    Page<Invoices> findAllBySupplierNameContainingIgnoreCase(String supplierName, Pageable pageable);

    Page<Invoices> findAllByIssueDate(Date issueDate, Pageable pageable);

    Page<Invoices> findAllBySupplierPartyId_Id(Long supplierId, Pageable pageable);

    Page<Invoices> findAllBySupplierPartyId_IdAndInvoiceNumberContainingIgnoreCase(Long supplierId, String invoiceNumber, Pageable pageable);

    Page<Invoices> findAllBySupplierPartyId_IdAndClientNameContainingIgnoreCase(Long supplierId, String clientName, Pageable pageable);

    Page<Invoices> findAllBySupplierPartyId_IdAndIssueDate(Long supplierId, Date issueDate, Pageable pageable);

    long countByInvoiceNumber(String invoiceNumber);

    @EntityGraph(attributePaths = {"invoiceItemsCollection", "templateId"})
    List<Invoices> findAllByStatusIgnoreCase(String status, Sort sort);

    @Query("SELECT COALESCE(MAX(i.invoiceNumberInt), 0) FROM Invoices i WHERE i.supplierPartyId.id = :supplierId")
    long findMaxInvoiceNumberIntBySupplierId(@Param("supplierId") Long supplierId);

    long countBySupplierPartyId_Id(Long supplierId);

    long countByClientPartyId_Id(Long clientId);

    // date range queries for bulk export
    List<Invoices> findAllBySupplierPartyId_IdAndIssueDateBetween(Long supplierId, Date startDate, Date endDate, Sort sort);

    long countBySupplierPartyId_IdAndIssueDateBetween(Long supplierId, Date startDate, Date endDate);

    @Query("SELECT MIN(i.issueDate) FROM Invoices i WHERE i.supplierPartyId.id = :supplierId")
    Date findEarliestIssueDateBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT MAX(i.issueDate) FROM Invoices i WHERE i.supplierPartyId.id = :supplierId")
    Date findLatestIssueDateBySupplierId(@Param("supplierId") Long supplierId);
}
