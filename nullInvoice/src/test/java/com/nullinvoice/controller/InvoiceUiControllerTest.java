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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceUiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InvoiceTemplateService invoiceTemplateService;

    @Autowired
    private PartyProfileService partyProfileService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    void generateInvoice_withUnpaidFlag_setsUnpaidStatus() throws Exception {
        // create a template and supplier, then request an unpaid invoice
        invoiceTemplateService.saveTemplate(buildTemplateForm("UI Template", true));
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("UI Supplier"));

        GenerateInvoiceRequest request = buildInvoiceRequest(supplier.getId(), "EUR");
        request.setDueDate(LocalDate.now().plusDays(14));

        mockMvc.perform(post("/invoices/generate")
                        .param("unpaid", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unpaid"))
                .andExpect(jsonPath("$.invoiceNumber").isNotEmpty());

        Invoices saved = invoiceRepository.findAll().get(0);
        assertThat(saved.getStatus()).isEqualTo(InvoiceService.STATUS_UNPAID);
    }

    @Test
    void generateInvoice_unpaidWithoutDueDate_returnsBadRequest() throws Exception {
        // unpaid invoices must include a due date
        invoiceTemplateService.saveTemplate(buildTemplateForm("UI Template", true));
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("UI Supplier"));

        GenerateInvoiceRequest request = buildInvoiceRequest(supplier.getId(), "EUR");

        mockMvc.perform(post("/invoices/generate")
                        .param("unpaid", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("due_date is required to mark invoice unpaid"));
    }

    @Test
    void markInvoiceIssued_fromUnpaid_updatesStatus() throws Exception {
        // seed an unpaid invoice and confirm the UI endpoint marks it as issued
        invoiceTemplateService.saveTemplate(buildTemplateForm("UI Template", true));
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("UI Supplier"));

        GenerateInvoiceRequest request = buildInvoiceRequest(supplier.getId(), "EUR");
        request.setDueDate(LocalDate.now().plusDays(7));
        Invoices unpaid = invoiceService.generateInvoice(request, true);

        mockMvc.perform(post("/invoices/{invoiceNumber}/mark-issued", unpaid.getInvoiceNumber())
                        .param("supplierId", supplier.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/invoices/" + unpaid.getInvoiceNumber() + "?supplierId=" + supplier.getId()));

        Invoices updated = invoiceRepository.findById(unpaid.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(InvoiceService.STATUS_ISSUED);
    }

    private GenerateInvoiceRequest buildInvoiceRequest(Long supplierId, String currencyCode) {
        GenerateInvoiceRequest request = new GenerateInvoiceRequest();
        request.setSupplierId(supplierId);
        request.setCurrencyCode(currencyCode);
        request.setClient(InvoicePartyDto.builder()
                .name("UI Client")
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
        dto.setTaxId("BG555000111");
        dto.setAddressLine1("1 Supplier St");
        dto.setCity("Sofia");
        dto.setCountry("BG");
        return dto;
    }
}
