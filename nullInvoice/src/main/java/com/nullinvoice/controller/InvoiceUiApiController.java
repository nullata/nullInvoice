// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.dto.GenerateInvoiceResponse;
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.service.InvoiceMapper;
import com.nullinvoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// UI-only generation endpoint: enables unpaid invoices while keeping public API contract unchanged
@Hidden
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceUiApiController {

    private final InvoiceService invoiceService;
    private final InvoiceMapper mapper;

    @PostMapping("/generate")
    public ResponseEntity<GenerateInvoiceResponse> generateInvoice(
            @Valid @RequestBody GenerateInvoiceRequest request,
            @RequestParam(name = "unpaid", defaultValue = "false") boolean unpaid) {
        // UI calls should only request the number response type
        String responseType = request.getResponseType() == null ? "number" : request.getResponseType().toLowerCase(Locale.ROOT);
        if (!responseType.isBlank() && !responseType.equals("number")) {
            throw new IllegalArgumentException("response_type must be 'number'");
        }

        Invoices invoice = invoiceService.generateInvoice(request, unpaid);
        GenerateInvoiceResponse response = GenerateInvoiceResponse.builder()
                .status(invoice.getStatus())
                .message("invoice generated")
                .invoiceNumber(invoice.getInvoiceNumber())
                .issueDate(mapper.toLocalDate(invoice.getIssueDate()))
                .build();

        return ResponseEntity.ok(response);
    }
}
