// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Data;

@Data
/**
 * Party profile CRUD (suppliers/clients)
 * @NotBlank validation + supplier-specific fields
 */
public class PartyDto {

    private Long id;
    @NotBlank
    private String name;
    private String taxId;
    private String vatId;
    @NotBlank
    private String addressLine1;
    private String addressLine2;
    @NotBlank
    private String city;
    private String region;
    private String postalCode;
    @NotBlank
    private String country;
    private String email;
    private String phone;

    // localization fields (suppliers)
    private String localeCode;
    private String localeLanguage;
    private String localeCountry;
    private String currencyCode;
    private String datePattern;
    private Long invoiceStartNumber;
    private String invoicePrefix;
    private Integer invoiceNumberDigits;
    private Long defaultTemplateId;
    private Long lastInvoiceNumber;
    private BigDecimal defaultTaxRate;
}
