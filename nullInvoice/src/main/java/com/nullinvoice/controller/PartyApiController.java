// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.service.PartyProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
@Tag(name = "Parties", description = "Client and supplier lookup/search")
public class PartyApiController {

    private static final int MIN_SEARCH_LENGTH = 2;

    private final PartyProfileService partyProfileService;

    @GetMapping("/client")
    @Operation(summary = "Find client by tax or VAT id")
    public PartyDto findClient(@RequestParam(required = false) String taxId,
            @RequestParam(required = false) String vatId) {
        return partyProfileService.findClientByTaxOrVatRequired(taxId, vatId);
    }

    @GetMapping("/clients/search")
    @Operation(summary = "Search clients")
    public ResponseEntity<List<PartyDto>> searchClients(@Parameter(description = "Search query") @RequestParam String q) {
        return ResponseEntity.ok(partyProfileService.searchClientsWithMinLength(q, MIN_SEARCH_LENGTH));
    }

    @GetMapping("/suppliers")
    @Operation(summary = "List suppliers")
    public ResponseEntity<List<PartyDto>> listSuppliers() {
        return ResponseEntity.ok(partyProfileService.listSuppliers());
    }
}
