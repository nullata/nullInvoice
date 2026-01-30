// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Unit tests for SearchParameterService covering search type normalization,
 * date query parsing, sort field validation, and pageable construction
 */
class SearchParameterServiceTest {

    private SearchParameterService service;

    @BeforeEach
    void setUp() {
        service = new SearchParameterService();
    }

    // === normalizeSearchType ===
    @ParameterizedTest
    @ValueSource(strings = {"number", "NUMBER", "Number", " number "})
    void normalizeSearchType_number_returnsNumber(String input) {
        // valid "number" type in various cases should normalize to lowercase
        assertThat(service.normalizeSearchType(input.trim())).isEqualTo("number");
    }

    @ParameterizedTest
    @ValueSource(strings = {"date", "DATE", "Date"})
    void normalizeSearchType_date_returnsDate(String input) {
        // "date" should normalize to lowercase for downstream handling
        assertThat(service.normalizeSearchType(input)).isEqualTo("date");
    }

    @ParameterizedTest
    @ValueSource(strings = {"client", "CLIENT", "Client"})
    void normalizeSearchType_client_returnsClient(String input) {
        // "client" should normalize to lowercase for downstream handling
        assertThat(service.normalizeSearchType(input)).isEqualTo("client");
    }

    @ParameterizedTest
    @ValueSource(strings = {"supplier", "SUPPLIER", "Supplier"})
    void normalizeSearchType_supplier_returnsSupplier(String input) {
        // "supplier" should normalize to lowercase for downstream handling
        assertThat(service.normalizeSearchType(input)).isEqualTo("supplier");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "unknown", "foo", "  "})
    void normalizeSearchType_invalidOrNull_returnsNumber(String input) {
        // Invalid or missing search types should default to "number"
        assertThat(service.normalizeSearchType(input)).isEqualTo("number");
    }

    // === normalizeSearchQuery ===
    @Test
    void normalizeSearchQuery_nonDateType_returnsQueryUnchanged() {
        // non-date search types should pass the query through unchanged
        assertThat(service.normalizeSearchQuery("number", "INV-001")).isEqualTo("INV-001");
        assertThat(service.normalizeSearchQuery("client", "Acme Corp")).isEqualTo("Acme Corp");
        assertThat(service.normalizeSearchQuery("supplier", "Supplier Inc")).isEqualTo("Supplier Inc");
    }

    @Test
    void normalizeSearchQuery_dateType_isoFormat_returnsIsoFormat() {
        // ISO date format should be recognized and returned as-is
        assertThat(service.normalizeSearchQuery("date", "2024-06-15")).isEqualTo("2024-06-15");
    }

    @Test
    void normalizeSearchQuery_dateType_bulgarianFormat_convertsToIso() {
        // bulgarian dd.MM.yyyy format should be converted to ISO format
        assertThat(service.normalizeSearchQuery("date", "15.06.2024")).isEqualTo("2024-06-15");
    }

    @Test
    void normalizeSearchQuery_dateType_invalidFormat_returnsOriginal() {
        // unparseable date strings should be returned unchanged.
        assertThat(service.normalizeSearchQuery("date", "June 15, 2024")).isEqualTo("June 15, 2024");
        assertThat(service.normalizeSearchQuery("date", "invalid")).isEqualTo("invalid");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void normalizeSearchQuery_blankQuery_returnsEmpty(String query) {
        // blank queries should normalize to empty string regardless of type
        assertThat(service.normalizeSearchQuery("date", query)).isEqualTo("");
        assertThat(service.normalizeSearchQuery("number", query)).isEqualTo("");
    }

    @Test
    void normalizeSearchQuery_nullType_treatsAsNonDate() {
        // null type should not trigger date parsing.
        assertThat(service.normalizeSearchQuery(null, "15.06.2024")).isEqualTo("15.06.2024");
    }

    // === normalizeSortBy ===
    @ParameterizedTest
    @ValueSource(strings = {"invoiceNumber", "issueDate", "clientName", "supplierName", "createdAt", "status"})
    void normalizeSortBy_validFields_returnsField(String field) {
        // whitelisted sort fields should be accepted
        assertThat(service.normalizeSortBy(field)).isEqualTo(field);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "id", "total", "  "})
    void normalizeSortBy_invalidOrNull_returnsCreatedAt(String field) {
        // invalid or missing sort fields should default to "createdAt"
        assertThat(service.normalizeSortBy(field)).isEqualTo("createdAt");
    }

    // === normalizeSortDir ===
    @ParameterizedTest
    @ValueSource(strings = {"asc", "ASC", "Asc", " asc "})
    void normalizeSortDir_asc_returnsAsc(String dir) {
        // ascending direction in various cases should normalize to "asc"
        assertThat(service.normalizeSortDir(dir.trim())).isEqualTo("asc");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"desc", "DESC", "Desc", "invalid", "xyz"})
    void normalizeSortDir_descOrInvalid_returnsDesc(String dir) {
        // descending or invalid directions should default to "desc"
        assertThat(service.normalizeSortDir(dir)).isEqualTo("desc");
    }

    // === buildSort ===
    @Test
    void buildSort_ascDirection_returnsSortAscending() {
        Sort sort = service.buildSort("invoiceNumber", "asc");

        // verify the sort is configured for ascending order on the correct field
        assertThat(sort.getOrderFor("invoiceNumber")).isNotNull();
        assertThat(sort.getOrderFor("invoiceNumber").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void buildSort_descDirection_returnsSortDescending() {
        Sort sort = service.buildSort("issueDate", "desc");

        // verify the sort is configured for descending order on the correct field.
        assertThat(sort.getOrderFor("issueDate")).isNotNull();
        assertThat(sort.getOrderFor("issueDate").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void buildSort_invalidField_usesCreatedAt() {
        // invalid field should fall back to default "createdAt"
        Sort sort = service.buildSort("invalid", "asc");

        assertThat(sort.getOrderFor("createdAt")).isNotNull();
    }

    @Test
    void buildSort_statusHonorsSortDirection() {
        // status sorting should respect sort direction for issued/unpaid ordering
        Sort ascSort = service.buildSort("status", "asc");
        Sort descSort = service.buildSort("status", "desc");

        assertThat(ascSort.getOrderFor("status")).isNotNull();
        assertThat(ascSort.getOrderFor("status").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(descSort.getOrderFor("status")).isNotNull();
        assertThat(descSort.getOrderFor("status").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    // === buildPageable ===
    @Test
    void buildPageable_validParams_returnsPageable() {
        Pageable pageable = service.buildPageable(2, 20, "invoiceNumber", "asc");

        // verify all pageable properties are set correctly
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("invoiceNumber")).isNotNull();
    }

    @Test
    void buildPageable_sizeExceedsMax_capsAtMax() {
        // page size should be capped at 100 to prevent excessive queries
        Pageable pageable = service.buildPageable(0, 500, "createdAt", "desc");

        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void buildPageable_nullSortParams_usesDefaults() {
        // null sort parameters should use defaults (createdAt, desc)
        Pageable pageable = service.buildPageable(0, 10, null, null);

        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
