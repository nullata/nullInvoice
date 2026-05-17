// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.dto.InvoiceRequestResponse;
import com.nullinvoice.entity.InvoiceRequest;
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.repository.InvoiceRepository;
import com.nullinvoice.service.InvoiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoice-requests")
@RequiredArgsConstructor
@Tag(name = "Invoice Requests", description = "Async invoice generation queue")
public class InvoiceRequestController {

    private final InvoiceRequestService invoiceRequestService;
    private final InvoiceRepository invoiceRepository;

    @PostMapping
    @Operation(summary = "Queue an invoice generation request",
            description = "Persists the request to the queue immediately and returns a request id. The invoice is generated asynchronously by a background worker.")
    public ResponseEntity<InvoiceRequestResponse> submit(@Valid @RequestBody GenerateInvoiceRequest request) {
        InvoiceRequest saved = invoiceRequestService.submitRequest(request);
        InvoiceRequestResponse response = InvoiceRequestResponse.builder()
                .requestId(saved.getId())
                .status(saved.getStatus())
                .message("invoice generation request queued")
                .createdAt(saved.getCreatedAt())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{requestId}")
    @Operation(summary = "Get queue request status",
            description = "Returns current status (PENDING, COMPLETED, FAILED). When COMPLETED, includes invoiceId and invoiceNumber.")
    public ResponseEntity<InvoiceRequestResponse> getStatus(
            @Parameter(description = "Request id", required = true)
            @PathVariable Long requestId) {
        InvoiceRequest req = invoiceRequestService.getRequest(requestId);
        InvoiceRequestResponse.InvoiceRequestResponseBuilder body = InvoiceRequestResponse.builder()
                .requestId(req.getId())
                .status(req.getStatus())
                .attempts(req.getAttempts())
                .createdAt(req.getCreatedAt())
                .completedAt(req.getCompletedAt())
                .errorMessage(req.getErrorMessage());
        if (InvoiceRequest.STATUS_COMPLETED.equals(req.getStatus()) && req.getInvoiceId() != null) {
            body.invoiceId(req.getInvoiceId());
            invoiceRepository.findById(req.getInvoiceId())
                    .map(Invoices::getInvoiceNumber)
                    .ifPresent(body::invoiceNumber);
        }
        return ResponseEntity.ok(body.build());
    }

}
