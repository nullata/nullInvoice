// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class GenerateInvoiceItemDto {

    @NotBlank
    @Schema(description = "Line item description", required = true, example = "Consulting services")
    private String description;

    @NotNull
    @DecimalMin("0.0001")
    @Schema(description = "Quantity", required = true, example = "10")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.00")
    @Schema(description = "Unit price", required = true, example = "150.00")
    private BigDecimal unitPrice;

    @DecimalMin("0.00")
    @Schema(description = "Discount amount (optional, defaults to 0)", example = "0")
    private BigDecimal discount;

    @DecimalMin("0.00")
    @Schema(description = "Tax rate as decimal, e.g. 0.20 for 20% (optional, defaults to supplier's default tax rate)", example = "0.20")
    private BigDecimal taxRate;
}
