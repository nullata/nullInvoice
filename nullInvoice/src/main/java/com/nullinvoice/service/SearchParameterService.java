// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Service for normalizing and validating search/sort parameters.
 */
@Service
public class SearchParameterService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIR = "desc";
    private static final String DEFAULT_SEARCH_TYPE = "number";

    /**
     * Builds a Pageable with normalized sort parameters
     * @param page
     * @param size
     * @param sortBy
     * @param sortDir
     * @return
     */
    public Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), buildSort(sortBy, sortDir));
    }

    /**
     * Builds a Sort object from sort parameters
     * @param sortBy
     * @param sortDir
     * @return
     */
    public Sort buildSort(String sortBy, String sortDir) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        Sort.Direction direction = normalizeDirection(sortDir);
        return Sort.by(direction, normalizedSortBy);
    }

    /**
     * Normalizes the search type to a valid value
     * @param type
     * @return
     */
    public String normalizeSearchType(String type) {
        if (type == null) {
            return DEFAULT_SEARCH_TYPE;
        }
        String normalized = type.trim().toLowerCase();
        return switch (normalized) {
            case "date", "client", "supplier", "number" -> normalized;
            default -> DEFAULT_SEARCH_TYPE;
        };
    }

    /**
     * Normalizes search query, with special handling for date type
     * @param type
     * @param query
     * @return
     */
    public String normalizeSearchQuery(String type, String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        if (type == null || !type.equalsIgnoreCase("date")) {
            return query;
        }

        String trimmed = query.trim();
        // try ISO format first
        try {
            LocalDate parsed = LocalDate.parse(trimmed);
            return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            // try dd.MM.yyyy format used in the UI
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate parsed = LocalDate.parse(trimmed, formatter);
            return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            return trimmed;
        }
    }

    /**
     * Normalizes the sort field to a valid column name
     * @param sortBy
     * @return
     */
    public String normalizeSortBy(String sortBy) {
        if (sortBy == null) {
            return DEFAULT_SORT_BY;
        }
        String normalized = sortBy.trim();
        return switch (normalized) {
            case "invoiceNumber", "issueDate", "clientName", "supplierName", "createdAt", "status" -> normalized;
            default -> DEFAULT_SORT_BY;
        };
    }

    /**
     * Normalizes the sort direction.
     * @param sortDir
     * @return
     */
    public String normalizeSortDir(String sortDir) {
        if (sortDir == null) {
            return DEFAULT_SORT_DIR;
        }
        String normalized = sortDir.trim().toLowerCase();
        return normalized.equals("asc") ? "asc" : DEFAULT_SORT_DIR;
    }

    private Sort.Direction normalizeDirection(String sortDir) {
        return normalizeSortDir(sortDir).equals("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
    }
}
