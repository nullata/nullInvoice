// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.repository;

import com.nullinvoice.entity.Parties;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartyProfileRepository extends JpaRepository<Parties, Long> {

    Optional<Parties> findFirstByRoleAndTaxId(String role, String taxId);

    Optional<Parties> findFirstByRoleAndVatId(String role, String vatId);

    Optional<Parties> findFirstByRoleOrderByIdAsc(String role);

    List<Parties> findAllByRoleOrderByNameAsc(String role);

    Optional<Parties> findByIdAndRole(Long id, String role);

    long countByRole(String role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Parties p WHERE p.id = :id")
    Optional<Parties> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Parties p WHERE p.id = :id AND p.role = :role AND p.deleted = false")
    Optional<Parties> findByIdAndRoleForUpdate(@Param("id") Long id, @Param("role") String role);

    @Query("SELECT p FROM Parties p WHERE p.role = :role AND "
            + "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "p.taxId LIKE CONCAT('%', :query, '%') OR "
            + "p.vatId LIKE CONCAT('%', :query, '%'))")
    List<Parties> searchByRoleAndQuery(@Param("role") String role, @Param("query") String query);

    // soft delete aware queries
    List<Parties> findAllByRoleAndDeletedFalseOrderByNameAsc(String role);

    Optional<Parties> findByIdAndRoleAndDeletedFalse(Long id, String role);

    Optional<Parties> findFirstByRoleAndDeletedFalseOrderByIdAsc(String role);

    long countByRoleAndDeletedFalse(String role);

    @Query("SELECT p FROM Parties p WHERE p.role = :role AND p.deleted = false AND "
            + "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR "
            + "p.taxId LIKE CONCAT('%', :query, '%') OR "
            + "p.vatId LIKE CONCAT('%', :query, '%'))")
    List<Parties> searchByRoleAndQueryAndDeletedFalse(@Param("role") String role, @Param("query") String query);

    @Query("SELECT p FROM Parties p WHERE p.role = :role AND p.deleted = false AND "
            + "LOWER(p.name) = LOWER(:name) AND "
            + "((:taxId IS NOT NULL AND p.taxId = :taxId) OR (:vatId IS NOT NULL AND p.vatId = :vatId)) AND "
            + "(:excludeId IS NULL OR p.id <> :excludeId)")
    Optional<Parties> findFirstByRoleAndDeletedFalseAndNameAndTaxOrVat(@Param("role") String role,
            @Param("name") String name,
            @Param("taxId") String taxId,
            @Param("vatId") String vatId,
            @Param("excludeId") Long excludeId);

    // for upsert - include deleted parties to allow reactivation
    Optional<Parties> findFirstByRoleAndTaxIdAndDeletedFalse(String role, String taxId);

    Optional<Parties> findFirstByRoleAndVatIdAndDeletedFalse(String role, String vatId);
}
