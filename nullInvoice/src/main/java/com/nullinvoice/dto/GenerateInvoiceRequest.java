// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Data
public class GenerateInvoiceRequest {

    @JsonAlias("response_type")
    @Schema(description = "Response type: 'number' returns JSON (default), 'pdf' returns multipart with PDF",
            allowableValues = {"number", "pdf"}, example = "number")
    private String responseType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Invoice issue date (optional, defaults to today)", example = "2026-01-16")
    private LocalDate issueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Invoice due date (optional)", example = "2026-01-30")
    private LocalDate dueDate;

    @NotNull(message = "supplierId is required")
    @JsonAlias("supplier_id")
    @Schema(description = "Supplier party ID", required = true, example = "1")
    private Long supplierId;

    @NotNull(message = "client is required")
    @Valid
    @Schema(description = "Client information - provide id to reference existing client, or fill in fields for new client")
    private InvoicePartyDto client;

    @NotEmpty
    @Valid
    @Schema(description = "Invoice line items")
    private List<GenerateInvoiceItemDto> items;

    @JsonAlias("currency_code")
    @Schema(description = "Currency code (optional, defaults to supplier's configured currency)", example = "EUR")
    private String currencyCode;

    @Schema(description = "Additional notes to include on the invoice")
    private String notes;

    @AssertTrue(message = "client.id or client details (name, addressLine1, city, country) required")
    private boolean isClientValid() {
        if (client == null) {
            return false;
        }
        if (client.getId() != null) {
            return true;
        }
        return isNotBlank(client.getName())
                && isNotBlank(client.getAddressLine1())
                && isNotBlank(client.getCity())
                && isNotBlank(client.getCountry());
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
