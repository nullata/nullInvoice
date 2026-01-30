// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long invoiceNumberInt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private String localeCode;
    private String currencyCode;
    private String status;
    private InvoicePartyDto supplier;
    private InvoicePartyDto client;
    private BigDecimal subtotal;
    private BigDecimal taxTotal;
    private BigDecimal total;
    private String notes;
    private List<InvoiceItemResponse> items;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
