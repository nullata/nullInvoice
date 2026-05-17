// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nullinvoice.dto.GenerateInvoiceItemDto;
import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.dto.InvoicePartyDto;
import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.dto.TemplateForm;
import com.nullinvoice.entity.InvoiceRequest;
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.repository.InvoiceRepository;
import com.nullinvoice.repository.InvoiceRequestRepository;
import com.nullinvoice.service.InvoiceQueueProcessor;
import com.nullinvoice.service.InvoiceRequestService;
import com.nullinvoice.service.InvoiceService;
import com.nullinvoice.service.InvoiceTemplateService;
import com.nullinvoice.service.PartyProfileService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the async invoice generation queue.
 *
 * The background worker bean is gated by nullinvoice.queue.enabled which is unset in
 * application-test.yml - so the worker is never wired up here. We drive the
 * processor directly to keep timing deterministic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceRequestApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceTemplateService invoiceTemplateService;

    @Autowired
    private PartyProfileService partyProfileService;

    @Autowired
    private InvoiceRequestService invoiceRequestService;

    @Autowired
    private InvoiceQueueProcessor invoiceQueueProcessor;

    @Autowired
    private InvoiceRequestRepository invoiceRequestRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Test
    void submit_validRequest_returns201WithPendingStatus() throws Exception {
        seedTemplate();
        Parties supplier = seedSupplier("Queue Test Supplier");

        GenerateInvoiceRequest request = buildInvoiceRequest(supplier.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/invoice-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.requestId").isNumber())
                .andExpect(jsonPath("$.message").value("invoice generation request queued"))
                .andReturn();

        Long requestId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("requestId").asLong();
        // confirm the row was actually persisted by the submit endpoint
        Optional<InvoiceRequest> stored = invoiceRequestRepository.findById(requestId);
        assertThat(stored).isPresent();
        assertThat(stored.get().getStatus()).isEqualTo("PENDING");
        assertThat(stored.get().getRequestPayload()).contains("\"supplierId\":" + supplier.getId());
    }

    @Test
    void submit_missingSupplierId_returnsBadRequest() throws Exception {
        seedTemplate();

        GenerateInvoiceRequest request = new GenerateInvoiceRequest();
        request.setClient(InvoicePartyDto.builder()
                .name("Client")
                .addressLine1("1 Street")
                .city("Sofia")
                .country("BG")
                .build());
        request.setItems(List.of(buildItemDto()));

        // validation on GenerateInvoiceRequest catches the missing supplierId before
        // the queue ever sees it - same behavior as the sync endpoint
        mockMvc.perform(post("/api/v1/invoice-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStatus_missingRequestId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/invoice-requests/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("invoice request not found: 99999"));
    }

    @Test
    void endToEnd_submitThenProcess_completesWithInvoiceNumber() throws Exception {
        seedTemplate();
        Parties supplier = seedSupplier("E2E Supplier");
        GenerateInvoiceRequest request = buildInvoiceRequest(supplier.getId());

        // 1. submit via the queue endpoint
        InvoiceRequest queued = invoiceRequestService.submitRequest(request);
        assertThat(queued.getStatus()).isEqualTo("PENDING");

        // 2. drive the worker manually (background worker bean is disabled in tests)
        invoiceQueueProcessor.processOne(queued.getId());

        // 3. status endpoint should now report COMPLETED with the invoice number
        mockMvc.perform(get("/api/v1/invoice-requests/" + queued.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.invoiceId").isNumber())
                .andExpect(jsonPath("$.invoiceNumber").isNotEmpty());

        // 4. the linked invoice is a normal Invoices row with the request_id set
        InvoiceRequest reloaded = invoiceRequestRepository.findById(queued.getId()).orElseThrow();
        assertThat(reloaded.getInvoiceId()).isNotNull();
        Invoices invoice = invoiceRepository.findById(reloaded.getInvoiceId()).orElseThrow();
        assertThat(invoice.getRequestId()).isEqualTo(queued.getId());
        assertThat(invoice.getInvoiceHtml()).isNotBlank();
    }

    @Test
    void processOneTwice_doesNotCreateDuplicateInvoice() throws Exception {
        // belt-and-suspenders idempotency: a second processOne on an already-completed
        // row should not generate a second invoice
        seedTemplate();
        Parties supplier = seedSupplier("Idempotency Supplier");
        GenerateInvoiceRequest request = buildInvoiceRequest(supplier.getId());
        InvoiceRequest queued = invoiceRequestService.submitRequest(request);

        invoiceQueueProcessor.processOne(queued.getId());
        long countAfterFirst = invoiceRepository.count();
        invoiceQueueProcessor.processOne(queued.getId());

        assertThat(invoiceRepository.count()).isEqualTo(countAfterFirst);
    }

    @Test
    void getStatus_pendingWithAttempts_surfacesAttemptsAndErrorMessage() throws Exception {
        // Confirms the GET endpoint exposes the failure-tracking columns. We seed the
        // row directly via the repository instead of calling recordFailure because
        // recordFailure uses REQUIRES_NEW: it opens a new tx that cannot see the
        // uncommitted row from this @Transactional test. The recordFailure logic
        // itself is exercised by InvoiceQueueProcessorTest.
        seedTemplate();
        Parties supplier = seedSupplier("Partial Failure Supplier");
        InvoiceRequest queued = invoiceRequestService.submitRequest(buildInvoiceRequest(supplier.getId()));
        queued.setAttempts(1);
        queued.setErrorMessage("transient blip");
        invoiceRequestRepository.save(queued);

        mockMvc.perform(get("/api/v1/invoice-requests/" + queued.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.attempts").value(1))
                .andExpect(jsonPath("$.errorMessage").value("transient blip"))
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                .andExpect(jsonPath("$.invoiceId").doesNotExist());
    }

    @Test
    void getStatus_failedTerminalState_surfacesFailedStatusAndCompletedAt() throws Exception {
        // Same rationale as above: drive the FAILED terminal-state response shape
        // without crossing the REQUIRES_NEW boundary.
        seedTemplate();
        Parties supplier = seedSupplier("Terminal Failure Supplier");
        InvoiceRequest queued = invoiceRequestService.submitRequest(buildInvoiceRequest(supplier.getId()));
        queued.setAttempts(3);
        queued.setStatus(InvoiceRequest.STATUS_FAILED);
        queued.setErrorMessage("permanent");
        queued.setCompletedAt(java.time.LocalDateTime.now());
        invoiceRequestRepository.save(queued);

        mockMvc.perform(get("/api/v1/invoice-requests/" + queued.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.attempts").value(3))
                .andExpect(jsonPath("$.errorMessage").value("permanent"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.invoiceId").doesNotExist())
                .andExpect(jsonPath("$.invoiceNumber").doesNotExist());
    }

    @Test
    void invoiceNumbers_areSequentialAcrossSyncAndAsyncPaths() throws Exception {
        // both paths go through InvoiceService.generateInvoice which holds the supplier
        // pessimistic lock - this asserts the lock actually does its job across the two
        // entry points. Interleave: async, sync, async and check numbers are 1,2,3.
        seedTemplate();
        Parties supplier = seedSupplier("Numbering Supplier");

        InvoiceRequest queued1 = invoiceRequestService.submitRequest(buildInvoiceRequest(supplier.getId()));
        invoiceQueueProcessor.processOne(queued1.getId());

        Invoices syncInvoice = invoiceService.generateInvoice(buildInvoiceRequest(supplier.getId()));

        InvoiceRequest queued3 = invoiceRequestService.submitRequest(buildInvoiceRequest(supplier.getId()));
        invoiceQueueProcessor.processOne(queued3.getId());

        Invoices first = invoiceRepository.findById(
                invoiceRequestRepository.findById(queued1.getId()).orElseThrow().getInvoiceId()
        ).orElseThrow();
        Invoices third = invoiceRepository.findById(
                invoiceRequestRepository.findById(queued3.getId()).orElseThrow().getInvoiceId()
        ).orElseThrow();

        assertThat(first.getInvoiceNumberInt()).isEqualTo(1L);
        assertThat(syncInvoice.getInvoiceNumberInt()).isEqualTo(2L);
        assertThat(third.getInvoiceNumberInt()).isEqualTo(3L);
        assertThat(first.getRequestId()).isEqualTo(queued1.getId());
        assertThat(syncInvoice.getRequestId()).isNull();
        assertThat(third.getRequestId()).isEqualTo(queued3.getId());
    }

    private void seedTemplate() {
        TemplateForm form = new TemplateForm();
        form.setName("Queue Test Template");
        form.setHtml("<html><body>queue</body></html>");
        form.setDefaultTemplate(true);
        invoiceTemplateService.saveTemplate(form);
    }

    private Parties seedSupplier(String name) {
        PartyDto dto = new PartyDto();
        dto.setName(name);
        dto.setTaxId("BG999888777");
        dto.setAddressLine1("1 Supplier St");
        dto.setCity("Sofia");
        dto.setCountry("BG");
        return partyProfileService.saveSupplierProfile(null, dto);
    }

    private GenerateInvoiceRequest buildInvoiceRequest(Long supplierId) {
        GenerateInvoiceRequest request = new GenerateInvoiceRequest();
        request.setSupplierId(supplierId);
        request.setCurrencyCode("EUR");
        request.setClient(InvoicePartyDto.builder()
                .name("Queue Client")
                .addressLine1("1 Client Way")
                .city("Sofia")
                .country("BG")
                .build());
        request.setItems(List.of(buildItemDto()));
        return request;
    }

    private GenerateInvoiceItemDto buildItemDto() {
        GenerateInvoiceItemDto item = new GenerateInvoiceItemDto();
        item.setDescription("Consulting");
        item.setQuantity(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setTaxRate(new BigDecimal("0.20"));
        return item;
    }
}
