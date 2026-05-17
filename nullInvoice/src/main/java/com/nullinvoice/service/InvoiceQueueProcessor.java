// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.entity.InvoiceRequest;
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.repository.InvoiceRepository;
import com.nullinvoice.repository.InvoiceRequestRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceQueueProcessor {

    private static final int ERROR_MESSAGE_MAX = 1024;

    private final InvoiceRequestRepository requestRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processOne(Long requestId) throws Exception {
        InvoiceRequest req = requestRepository.findByIdForUpdate(requestId).orElse(null);
        if (req == null || !InvoiceRequest.STATUS_PENDING.equals(req.getStatus())) {
            return;
        }

        // Idempotency: if a prior partial run created the invoice, link and complete without re-rendering.
        Optional<Invoices> existing = invoiceRepository.findByRequestId(req.getId());
        if (existing.isPresent()) {
            Invoices inv = existing.get();
            req.setStatus(InvoiceRequest.STATUS_COMPLETED);
            req.setInvoiceId(inv.getId());
            req.setCompletedAt(LocalDateTime.now());
            return;
        }

        GenerateInvoiceRequest payload = objectMapper.readValue(
                req.getRequestPayload(), GenerateInvoiceRequest.class);

        Invoices saved = invoiceService.generateInvoice(payload, req.getId());

        req.setStatus(InvoiceRequest.STATUS_COMPLETED);
        req.setInvoiceId(saved.getId());
        req.setCompletedAt(LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long requestId, Throwable ex) {
        InvoiceRequest req = requestRepository.findById(requestId).orElse(null);
        if (req == null) {
            return;
        }
        int attempts = (req.getAttempts() == null ? 0 : req.getAttempts()) + 1;
        req.setAttempts(attempts);
        req.setErrorMessage(truncate(ex.getMessage(), ERROR_MESSAGE_MAX));
        int max = req.getMaxAttempts() == null ? 0 : req.getMaxAttempts();
        if (attempts >= max) {
            req.setStatus(InvoiceRequest.STATUS_FAILED);
            req.setCompletedAt(LocalDateTime.now());
        }
        log.warn("invoice request {} failed (attempt {}/{}): {}",
                requestId, attempts, max, ex.getMessage());
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
