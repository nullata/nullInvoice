// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Party (supplier/client) information. Provide id to reference existing party, or fill in fields to create/update.")
public class InvoicePartyDto {

    @Schema(description = "Existing party ID. If provided for client, other fields are ignored and party is looked up.", example = "1")
    private Long id;

    @Schema(description = "Party name (required if no id)", example = "Acme Corp")
    private String name;

    @Schema(description = "Tax identification number", example = "123456789")
    private String taxId;

    @Schema(description = "VAT identification number", example = "BG123456789")
    private String vatId;

    @Schema(description = "Address line 1 (required if no id)", example = "123 Main Street")
    private String addressLine1;

    @Schema(description = "Address line 2", example = "Suite 100")
    private String addressLine2;

    @Schema(description = "City (required if no id)", example = "Sofia")
    private String city;

    @Schema(description = "Region/State/Province", example = "Sofia")
    private String region;

    @Schema(description = "Postal/ZIP code", example = "1000")
    private String postalCode;

    @Schema(description = "Country (required if no id)", example = "Bulgaria")
    private String country;

    @Schema(description = "Email address", example = "billing@acme.com")
    private String email;

    @Schema(description = "Phone number", example = "+359 2 123 4567")
    private String phone;
}
