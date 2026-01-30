// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.config;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

public class InvoiceLocale {

    private final String code;
    private final Locale locale;
    private final Currency currency;
    private final DateTimeFormatter dateFormatter;

    public InvoiceLocale(String code, Locale locale, Currency currency, DateTimeFormatter dateFormatter) {
        this.code = code;
        this.locale = locale;
        this.currency = currency;
        this.dateFormatter = dateFormatter;
    }

    public String getCode() {
        return code;
    }

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    public String formatDate(LocalDate value) {
        return value.format(dateFormatter);
    }

    public String formatMoney(BigDecimal amount) {
        return formatMoney(amount, currency.getCurrencyCode());
    }

    public String formatMoney(BigDecimal amount, String currencyCode) {
        Currency resolvedCurrency = resolveCurrency(currencyCode);
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(resolvedCurrency);
        int fractionDigits = resolvedCurrency.getDefaultFractionDigits();
        if (fractionDigits < 0) {
            fractionDigits = 2;
        }
        format.setMaximumFractionDigits(fractionDigits);
        format.setMinimumFractionDigits(fractionDigits);
        return format.format(amount);
    }

    private Currency resolveCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return currency;
        }
        return Currency.getInstance(currencyCode.trim().toUpperCase(Locale.ROOT));
    }
}
