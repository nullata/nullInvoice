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
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.repository.InvoiceRepository;
import com.nullinvoice.service.InvoiceService;
import com.nullinvoice.service.InvoiceTemplateService;
import com.nullinvoice.service.PartyProfileService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceTemplateService invoiceTemplateService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PartyProfileService partyProfileService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void generateInvoice_withKnownSupplier_returnsInvoiceNumber() throws Exception {
        // ensure there is a default template and a valid supplier for invoice generation
        invoiceTemplateService.saveTemplate(buildTemplateForm("Default API Template", true));
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("API Supplier"));

        GenerateInvoiceRequest request = buildInvoiceRequest(supplier.getId(), "EUR");
        request.setDueDate(LocalDate.now().plusDays(14));

        // call the API and verify a successful response with an invoice number
        mockMvc.perform(post("/api/v1/invoices/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("issued"))
                .andExpect(jsonPath("$.message").value("invoice generated"))
                .andExpect(jsonPath("$.invoiceNumber").isNotEmpty());
        // confirm the invoice was persisted with rendered HTML
        List<Invoices> invoices = invoiceRepository.findAll();
        assertThat(invoices).hasSize(1);
        assertThat(invoices.get(0).getInvoiceHtml()).isNotBlank();
    }

    @Test
    void generateInvoice_withUnknownSupplier_returnsBadRequest() throws Exception {
        // seed a default template so only the supplier lookup fails
        invoiceTemplateService.saveTemplate(buildTemplateForm("Default API Template", true));

        GenerateInvoiceRequest request = buildInvoiceRequest(9999L, "USD");

        // the API should reject a request referencing a non-existent supplier
        mockMvc.perform(post("/api/v1/invoices/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("supplier not found"));
    }

    @Test
    void generateInvoice_withInvalidCurrency_returnsBadRequest() throws Exception {
        // seed template and supplier, then submit an invalid currency code
        invoiceTemplateService.saveTemplate(buildTemplateForm("Default API Template", true));
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("API Supplier"));

        GenerateInvoiceRequest request = buildInvoiceRequest(supplier.getId(), "ZZZ");

        // the API should reject unknown currency codes with a clear message
        mockMvc.perform(post("/api/v1/invoices/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid currency_code: ZZZ"));
    }

    @Test
    void generateInvoice_withMissingClientDetails_returnsBadRequest() throws Exception {
        // seed template and supplier, then omit required client details
        invoiceTemplateService.saveTemplate(buildTemplateForm("Default API Template", true));
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("API Supplier"));

        GenerateInvoiceRequest request = new GenerateInvoiceRequest();
        request.setSupplierId(supplier.getId());
        request.setClient(InvoicePartyDto.builder().build());
        request.setItems(List.of(buildItemDto()));

        // the API should fail validation and explain the missing client info
        mockMvc.perform(post("/api/v1/invoices/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("client.id or client details")));
    }

    @Test
    void listInvoices_withStatusFilter_returnsOnlyUnpaid() throws Exception {
        // seed issued and unpaid invoices so the status filter can be verified
        invoiceTemplateService.saveTemplate(buildTemplateForm("Default API Template", true));
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("API Supplier"));

        GenerateInvoiceRequest issuedRequest = buildInvoiceRequest(supplier.getId(), "EUR");
        invoiceService.generateInvoice(issuedRequest);

        GenerateInvoiceRequest unpaidRequest = buildInvoiceRequest(supplier.getId(), "EUR");
        unpaidRequest.setDueDate(LocalDate.now().plusDays(7));
        invoiceService.generateInvoice(unpaidRequest, true);

        mockMvc.perform(get("/api/v1/invoices")
                .param("status", "unpaid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("unpaid"));
    }

    @Test
    void listInvoices_withInvalidStatus_returnsBadRequest() throws Exception {
        // invalid status values should be rejected
        mockMvc.perform(get("/api/v1/invoices")
                .param("status", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid status: unknown"));
    }

    private GenerateInvoiceRequest buildInvoiceRequest(Long supplierId, String currencyCode) {
        GenerateInvoiceRequest request = new GenerateInvoiceRequest();
        request.setSupplierId(supplierId);
        request.setCurrencyCode(currencyCode);
        request.setClient(InvoicePartyDto.builder()
                .name("API Client")
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

    private TemplateForm buildTemplateForm(String name, boolean isDefault) {
        TemplateForm form = new TemplateForm();
        form.setName(name);
        form.setHtml("<html><body>" + name + "</body></html>");
        form.setDefaultTemplate(isDefault);
        return form;
    }

    private PartyDto buildSupplierDto(String name) {
        PartyDto dto = new PartyDto();
        dto.setName(name);
        dto.setTaxId("BG222333444");
        dto.setAddressLine1("1 Supplier St");
        dto.setCity("Sofia");
        dto.setCountry("BG");
        return dto;
    }
}
