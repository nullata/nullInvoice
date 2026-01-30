// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.config;

import com.nullinvoice.entity.Parties;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Factory for creating InvoiceLocale instances from supplier settings. Falls
 * back to defaults for any missing supplier configuration.
 */
@Component
public class SupplierLocaleFactory {

    private static final long DEFAULT_START_NUMBER = 1L;
    private static final int DEFAULT_NUMBER_DIGITS = 6;
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_COUNTRY = "US";
    private static final String DEFAULT_DATE_PATTERN = "MM/dd/yyyy";
    private static final String DEFAULT_CURRENCY = "USD";

    private final InvoiceLocale defaultLocale;

    public SupplierLocaleFactory(InvoiceLocale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    /**
     * Creates an InvoiceLocale from supplier settings.Falls back to defaults
     * for any missing values.
     *
     * @param supplier
     * @return
     */
    public InvoiceLocale createFromSupplier(Parties supplier) {
        if (supplier == null || !hasAnyLocaleSettings(supplier)) {
            return defaultLocale;
        }

        String language = coalesce(supplier.getLocaleLanguage(), DEFAULT_LANGUAGE);
        String country = coalesce(supplier.getLocaleCountry(), DEFAULT_COUNTRY);
        String pattern = coalesce(supplier.getDatePattern(), DEFAULT_DATE_PATTERN);
        String currencyCode = coalesce(supplier.getCurrencyCode(), DEFAULT_CURRENCY);
        String localeCode = supplier.getLocaleCode();

        Locale locale = new Locale(language, country);
        DateTimeFormatter formatter;
        try {
            formatter = DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException ex) {
            formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN);
        }

        Currency currency = resolveCurrency(currencyCode, locale);
        if (localeCode == null || localeCode.isBlank()) {
            localeCode = language + "-" + country;
        }

        return new InvoiceLocale(localeCode, locale, currency, formatter);
    }

    /**
     * Gets the invoice start number for a supplier, falling back to 1.
     *
     * @param supplier
     * @return
     */
    public long getInvoiceStartNumber(Parties supplier) {
        if (supplier != null && supplier.getInvoiceStartNumber() != null) {
            return supplier.getInvoiceStartNumber();
        }
        return DEFAULT_START_NUMBER;
    }

    /**
     * Gets the invoice prefix for a supplier.Returns null if not configured.
     *
     * @param supplier
     * @return
     */
    public String getInvoicePrefix(Parties supplier) {
        if (supplier != null && isNotBlank(supplier.getInvoicePrefix())) {
            return supplier.getInvoicePrefix().trim();
        }
        return null;
    }

    /**
     * Gets the number of digits for invoice numbers, falling back to 6.
     *
     * @param supplier
     * @return
     */
    public int getInvoiceNumberDigits(Parties supplier) {
        if (supplier != null && supplier.getInvoiceNumberDigits() != null && supplier.getInvoiceNumberDigits() > 0) {
            return supplier.getInvoiceNumberDigits();
        }
        return DEFAULT_NUMBER_DIGITS;
    }

    private boolean hasAnyLocaleSettings(Parties supplier) {
        return isNotBlank(supplier.getLocaleCode())
                || isNotBlank(supplier.getLocaleLanguage())
                || isNotBlank(supplier.getLocaleCountry())
                || isNotBlank(supplier.getCurrencyCode())
                || isNotBlank(supplier.getDatePattern());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String coalesce(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback;
    }

    private Currency resolveCurrency(String currencyCode, Locale locale) {
        if (currencyCode != null && !currencyCode.isBlank()) {
            try {
                return Currency.getInstance(currencyCode.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                // fall back to locale default
            }
        }
        try {
            return Currency.getInstance(locale);
        } catch (IllegalArgumentException ex) {
            return Currency.getInstance(DEFAULT_CURRENCY);
        }
    }
}
