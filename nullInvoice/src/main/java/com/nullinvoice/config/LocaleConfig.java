// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.config;

import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates a default InvoiceLocale bean used as fallback when suppliers don't
 * have custom localization settings configured. Localization settings are
 * configured per-supplier in the UI.
 */
@Configuration
public class LocaleConfig {

    private static final String DEFAULT_CODE = "en-US";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String DEFAULT_COUNTRY = "US";
    private static final String DEFAULT_DATE_PATTERN = "MM/dd/yyyy";
    private static final String DEFAULT_CURRENCY = "USD";

    @Bean
    public InvoiceLocale invoiceLocale() {
        Locale locale = Locale.of(DEFAULT_LANGUAGE, DEFAULT_COUNTRY);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN);
        Currency currency = Currency.getInstance(DEFAULT_CURRENCY);

        return new InvoiceLocale(DEFAULT_CODE, locale, currency, formatter);
    }
}
