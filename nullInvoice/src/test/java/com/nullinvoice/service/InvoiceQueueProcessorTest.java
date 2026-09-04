// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.entity.InvoiceRequest;
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.repository.InvoiceRepository;
import com.nullinvoice.repository.InvoiceRequestRepository;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the queue processor's transaction-level logic: claim semantics,
 * idempotency, and failure recording. The end-to-end flow is exercised by
 * InvoiceRequestApiControllerTest.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceQueueProcessorTest {

    @Mock
    private InvoiceRequestRepository requestRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private ObjectMapper objectMapper;

    private InvoiceQueueProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new InvoiceQueueProcessor(
                requestRepository,
                invoiceRepository,
                invoiceService,
                objectMapper
        );
    }

    @Test
    void processOne_pendingRow_generatesInvoiceAndCompletes() throws Exception {
        InvoiceRequest req = newPendingRequest(42L, "{\"supplierId\":1}");
        GenerateInvoiceRequest payload = new GenerateInvoiceRequest();
        when(requestRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(req));
        when(invoiceRepository.findByRequestId(42L)).thenReturn(Optional.empty());
        when(objectMapper.readValue("{\"supplierId\":1}", GenerateInvoiceRequest.class)).thenReturn(payload);
        Invoices saved = new Invoices();
        saved.setId(7L);
        when(invoiceService.generateInvoice(payload, 42L)).thenReturn(saved);

        processor.processOne(42L);

        assertThat(req.getStatus()).isEqualTo(InvoiceRequest.STATUS_COMPLETED);
        assertThat(req.getInvoiceId()).isEqualTo(7L);
        assertThat(req.getCompletedAt()).isNotNull();
        verify(invoiceService).generateInvoice(payload, 42L);
    }

    @Test
    void processOne_rowNotFound_isNoOp() throws Exception {
        when(requestRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        processor.processOne(99L);

        verify(invoiceService, never()).generateInvoice(any(GenerateInvoiceRequest.class), any());
    }

    @Test
    void processOne_notPending_returnsEarly() throws Exception {
        // simulates a row a concurrent worker already completed; we must not regenerate
        InvoiceRequest req = newPendingRequest(42L, "{\"supplierId\":1}");
        req.setStatus(InvoiceRequest.STATUS_COMPLETED);
        when(requestRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(req));

        processor.processOne(42L);

        verify(invoiceRepository, never()).findByRequestId(any());
        verify(invoiceService, never()).generateInvoice(any(GenerateInvoiceRequest.class), any());
    }

    @Test
    void processOne_invoiceAlreadyExists_linksAndCompletesWithoutRegenerating() throws Exception {
        // idempotency safety net: if a prior partial run created the invoice, we must not create another
        InvoiceRequest req = newPendingRequest(42L, "{\"supplierId\":1}");
        when(requestRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(req));
        Invoices existing = new Invoices();
        existing.setId(101L);
        when(invoiceRepository.findByRequestId(42L)).thenReturn(Optional.of(existing));

        processor.processOne(42L);

        assertThat(req.getStatus()).isEqualTo(InvoiceRequest.STATUS_COMPLETED);
        assertThat(req.getInvoiceId()).isEqualTo(101L);
        verify(invoiceService, never()).generateInvoice(any(GenerateInvoiceRequest.class), any());
    }

    @Test
    void processOne_generateInvoiceThrows_propagates() throws Exception {
        // worker (caller) needs to see the exception so it can route to recordFailure
        InvoiceRequest req = newPendingRequest(42L, "{\"supplierId\":1}");
        GenerateInvoiceRequest payload = new GenerateInvoiceRequest();
        when(requestRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(req));
        when(invoiceRepository.findByRequestId(42L)).thenReturn(Optional.empty());
        when(objectMapper.readValue("{\"supplierId\":1}", GenerateInvoiceRequest.class)).thenReturn(payload);
        when(invoiceService.generateInvoice(payload, 42L))
                .thenThrow(new IllegalArgumentException("supplier not found"));

        try {
            processor.processOne(42L);
            assertThat(false).as("expected exception").isTrue();
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("supplier not found");
        }
    }

    @Test
    void recordFailure_belowMaxAttempts_incrementsAndKeepsPending() {
        InvoiceRequest req = newPendingRequest(42L, "{}");
        req.setAttempts(0);
        req.setMaxAttempts(3);
        when(requestRepository.findById(42L)).thenReturn(Optional.of(req));

        processor.recordFailure(42L, new RuntimeException("transient"));

        assertThat(req.getAttempts()).isEqualTo(1);
        assertThat(req.getStatus()).isEqualTo(InvoiceRequest.STATUS_PENDING);
        assertThat(req.getCompletedAt()).isNull();
        assertThat(req.getErrorMessage()).isEqualTo("transient");
    }

    @Test
    void recordFailure_atMaxAttempts_marksFailedAndSetsCompletedAt() {
        InvoiceRequest req = newPendingRequest(42L, "{}");
        req.setAttempts(2);
        req.setMaxAttempts(3);
        when(requestRepository.findById(42L)).thenReturn(Optional.of(req));

        processor.recordFailure(42L, new RuntimeException("permanent"));

        assertThat(req.getAttempts()).isEqualTo(3);
        assertThat(req.getStatus()).isEqualTo(InvoiceRequest.STATUS_FAILED);
        assertThat(req.getCompletedAt()).isNotNull();
    }

    @Test
    void recordFailure_truncatesLongErrorMessage() {
        InvoiceRequest req = newPendingRequest(42L, "{}");
        req.setAttempts(0);
        req.setMaxAttempts(3);
        when(requestRepository.findById(42L)).thenReturn(Optional.of(req));
        String longMessage = "x".repeat(2000);

        processor.recordFailure(42L, new RuntimeException(longMessage));

        assertThat(req.getErrorMessage()).hasSize(1024);
    }

    @Test
    void recordFailure_missingRow_isNoOp() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        processor.recordFailure(99L, new RuntimeException("doesn't matter"));

        verify(requestRepository, times(1)).findById(99L);
    }

    @Test
    void processOne_deserializesAndPropagatesTemplateIdAndName() throws Exception {
        // proves templateId + templateName survive the queue's Jackson round-trip and reach
        // InvoiceService.generateInvoice with the same values. Uses a real ObjectMapper so
        // any future field/alias change on the DTO would break this test.
        ObjectMapper realMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        InvoiceQueueProcessor realMapperProcessor = new InvoiceQueueProcessor(
                requestRepository, invoiceRepository, invoiceService, realMapper);

        // stored payload as a real API caller would send it (snake_case aliases + camelCase mix)
        String payloadJson = "{"
                + "\"supplier_id\":1,"
                + "\"template_id\":42,"
                + "\"templateName\":\"Queue Template\""
                + "}";

        InvoiceRequest req = newPendingRequest(50L, payloadJson);
        when(requestRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(req));
        when(invoiceRepository.findByRequestId(50L)).thenReturn(Optional.empty());
        Invoices saved = new Invoices();
        saved.setId(200L);
        when(invoiceService.generateInvoice(any(GenerateInvoiceRequest.class), eq(50L))).thenReturn(saved);

        realMapperProcessor.processOne(50L);

        ArgumentCaptor<GenerateInvoiceRequest> captor = ArgumentCaptor.forClass(GenerateInvoiceRequest.class);
        verify(invoiceService).generateInvoice(captor.capture(), eq(50L));
        assertThat(captor.getValue().getTemplateId()).isEqualTo(42L);
        assertThat(captor.getValue().getTemplateName()).isEqualTo("Queue Template");
        assertThat(captor.getValue().getSupplierId()).isEqualTo(1L);
    }

    private InvoiceRequest newPendingRequest(Long id, String payload) {
        InvoiceRequest req = new InvoiceRequest();
        req.setId(id);
        req.setStatus(InvoiceRequest.STATUS_PENDING);
        req.setSupplierId(1L);
        req.setRequestPayload(payload);
        req.setAttempts(0);
        req.setMaxAttempts(3);
        return req;
    }
}
