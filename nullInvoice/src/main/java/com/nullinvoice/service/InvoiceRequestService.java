// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.entity.InvoiceRequest;
import com.nullinvoice.error.InvoiceRequestNotFoundException;
import com.nullinvoice.repository.InvoiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceRequestService {

    private final InvoiceRequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public InvoiceRequest submitRequest(GenerateInvoiceRequest request) {
        InvoiceRequest entity = new InvoiceRequest();
        entity.setSupplierId(request.getSupplierId());
        entity.setRequestPayload(serializePayload(request));
        entity.setStatus(InvoiceRequest.STATUS_PENDING);
        return requestRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public InvoiceRequest getRequest(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new InvoiceRequestNotFoundException(requestId));
    }

    private String serializePayload(GenerateInvoiceRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("failed to serialize invoice request payload", ex);
        }
    }
}
