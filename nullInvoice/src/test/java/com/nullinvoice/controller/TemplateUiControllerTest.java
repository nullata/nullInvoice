// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.dto.TemplateForm;
import com.nullinvoice.entity.InvoiceTemplates;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.repository.InvoiceTemplateRepository;
import com.nullinvoice.service.InvoiceTemplateService;
import com.nullinvoice.service.PartyProfileService;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TemplateUiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceTemplateRepository templateRepository;

    @Autowired
    private InvoiceTemplateService invoiceTemplateService;

    @Autowired
    private PartyProfileService partyProfileService;

    @Test
    void saveTemplate_createsTemplateAndDefault() throws Exception {
        // submit a template form with default enabled and expect a redirect on success
        mockMvc.perform(post("/templates/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Default Template")
                .param("html", "<html>{{invoiceNumber}}</html>")
                .param("defaultTemplate", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/templates"));

        // verify the template exists and is marked as global default
        Optional<InvoiceTemplates> saved = templateRepository.findByNameIgnoreCase("Default Template");
        assertThat(saved).isPresent();
        assertThat(saved.get().getIsDefault()).isTrue();
    }

    @Test
    void saveTemplate_missingFields_returnsValidationErrors() throws Exception {
        // post invalid template data to verify validation and view rendering
        mockMvc.perform(post("/templates/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "")
                .param("html", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("templates"))
                .andExpect(model().attributeHasFieldErrors("templateForm", "name", "html"));

        // confirm no template was stored after the failed validation
        assertThat(templateRepository.findAll()).isEmpty();
    }

    @Test
    void setSupplierDefaultTemplate_updatesSupplierAndResolvesEffectiveTemplate() throws Exception {
        // create a supplier and two templates to test supplier-specific defaults
        Parties supplier = partyProfileService.saveSupplierProfile(null, buildSupplierDto("Template Supplier"));
        InvoiceTemplates globalDefault = invoiceTemplateService.saveTemplate(buildTemplateForm("Global Default", true));
        InvoiceTemplates supplierTemplate = invoiceTemplateService.saveTemplate(buildTemplateForm("Supplier Default", false));

        // set the supplier-specific default via the UI endpoint
        mockMvc.perform(post("/templates/default")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("supplierId", supplier.getId().toString())
                .param("templateId", supplierTemplate.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/templates"));

        // verify the supplier now points to the template we selected
        Parties updatedSupplier = partyProfileService.findSupplierEntityById(supplier.getId()).orElseThrow();
        assertThat(updatedSupplier.getDefaultTemplateId()).isNotNull();
        assertThat(updatedSupplier.getDefaultTemplateId().getId()).isEqualTo(supplierTemplate.getId());

        // confirm the effective template resolves to the supplier-specific choice
        PartyDto supplierDto = partyProfileService.findSupplierById(supplier.getId()).orElseThrow();
        InvoiceTemplates effective = invoiceTemplateService.resolveEffectiveTemplate(supplierDto);
        assertThat(effective.getId()).isEqualTo(supplierTemplate.getId());
        assertThat(globalDefault.getId()).isNotNull();
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
        dto.setTaxId("BG555666777");
        dto.setAddressLine1("1 Template Rd");
        dto.setCity("Sofia");
        dto.setCountry("BG");
        return dto;
    }
}
