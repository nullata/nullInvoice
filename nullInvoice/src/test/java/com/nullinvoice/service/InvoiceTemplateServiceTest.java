// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.dto.TemplateIndicator;
import com.nullinvoice.entity.InvoiceTemplates;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.repository.InvoiceTemplateRepository;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for InvoiceTemplateService template resolution and indicator logic
 */
@ExtendWith(MockitoExtension.class)
class InvoiceTemplateServiceTest {

    @Mock
    private InvoiceTemplateRepository repository;

    private InvoiceTemplateService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceTemplateService(repository);
    }

    // === getTemplateIndicator ===

    @Test
    void getTemplateIndicator_nullSupplier_returnsNullIndicator() {
        // null supplier should result in null indicator values
        TemplateIndicator indicator = service.getTemplateIndicator(null);

        assertThat(indicator.getTemplateName()).isNull();
        assertThat(indicator.getSourceKey()).isNull();
    }

    @Test
    void getTemplateIndicator_supplierWithNullId_returnsNullIndicator() {
        // supplier without persisted ID should return null indicator
        PartyDto supplier = new PartyDto();
        supplier.setId(null);

        TemplateIndicator indicator = service.getTemplateIndicator(supplier);

        assertThat(indicator.getTemplateName()).isNull();
        assertThat(indicator.getSourceKey()).isNull();
    }

    @Test
    void getTemplateIndicator_supplierWithTemplate_returnsSupplierSource() {
        // supplier with configured template should indicate "supplier" source
        InvoiceTemplates supplierTemplate = createTemplate(1L, "Supplier Template");
        when(repository.findById(1L)).thenReturn(Optional.of(supplierTemplate));

        PartyDto supplier = new PartyDto();
        supplier.setId(100L);
        supplier.setDefaultTemplateId(1L);

        TemplateIndicator indicator = service.getTemplateIndicator(supplier);

        assertThat(indicator.getTemplateName()).isEqualTo("Supplier Template");
        assertThat(indicator.getSourceKey()).isEqualTo("supplier");
    }

    @Test
    void getTemplateIndicator_supplierWithoutTemplate_fallsBackToGlobal() {
        // supplier without template should fall back to global default
        InvoiceTemplates globalTemplate = createTemplate(2L, "Global Template");
        when(repository.findFirstByIsDefaultTrue()).thenReturn(Optional.of(globalTemplate));

        PartyDto supplier = new PartyDto();
        supplier.setId(100L);
        supplier.setDefaultTemplateId(null);

        TemplateIndicator indicator = service.getTemplateIndicator(supplier);

        assertThat(indicator.getTemplateName()).isEqualTo("Global Template");
        assertThat(indicator.getSourceKey()).isEqualTo("global");
    }

    @Test
    void getTemplateIndicator_supplierTemplateNotFound_fallsBackToGlobal() {
        // if supplier's configured template no longer exists, fall back to global
        InvoiceTemplates globalTemplate = createTemplate(2L, "Global Template");
        when(repository.findById(999L)).thenReturn(Optional.empty());
        when(repository.findFirstByIsDefaultTrue()).thenReturn(Optional.of(globalTemplate));

        PartyDto supplier = new PartyDto();
        supplier.setId(100L);
        supplier.setDefaultTemplateId(999L);

        TemplateIndicator indicator = service.getTemplateIndicator(supplier);

        assertThat(indicator.getTemplateName()).isEqualTo("Global Template");
        assertThat(indicator.getSourceKey()).isEqualTo("global");
    }

    @Test
    void getTemplateIndicator_noTemplatesAtAll_returnsNullValues() {
        // when no templates exist, indicator should have null values
        when(repository.findFirstByIsDefaultTrue()).thenReturn(Optional.empty());

        PartyDto supplier = new PartyDto();
        supplier.setId(100L);
        supplier.setDefaultTemplateId(null);

        TemplateIndicator indicator = service.getTemplateIndicator(supplier);

        assertThat(indicator.getTemplateName()).isNull();
        assertThat(indicator.getSourceKey()).isNull();
    }

    // === resolveEffectiveTemplate ===

    @Test
    void resolveEffectiveTemplate_nullSupplier_returnsGlobalDefault() {
        // null supplier should use global default template
        InvoiceTemplates globalTemplate = createTemplate(1L, "Global");
        when(repository.findFirstByIsDefaultTrue()).thenReturn(Optional.of(globalTemplate));

        InvoiceTemplates result = service.resolveEffectiveTemplate(null);

        assertThat(result.getName()).isEqualTo("Global");
    }

    @Test
    void resolveEffectiveTemplate_supplierWithTemplate_returnsSupplierTemplate() {
        // supplier with configured template should use that template
        InvoiceTemplates supplierTemplate = createTemplate(1L, "Supplier Template");
        when(repository.findById(1L)).thenReturn(Optional.of(supplierTemplate));

        PartyDto supplier = new PartyDto();
        supplier.setDefaultTemplateId(1L);

        InvoiceTemplates result = service.resolveEffectiveTemplate(supplier);

        assertThat(result.getName()).isEqualTo("Supplier Template");
    }

    @Test
    void resolveEffectiveTemplate_supplierTemplateNotFound_returnsGlobalDefault() {
        // missing supplier template should fall back to global default
        InvoiceTemplates globalTemplate = createTemplate(2L, "Global");
        when(repository.findById(999L)).thenReturn(Optional.empty());
        when(repository.findFirstByIsDefaultTrue()).thenReturn(Optional.of(globalTemplate));

        PartyDto supplier = new PartyDto();
        supplier.setDefaultTemplateId(999L);

        InvoiceTemplates result = service.resolveEffectiveTemplate(supplier);

        assertThat(result.getName()).isEqualTo("Global");
    }

    @Test
    void resolveEffectiveTemplate_noTemplates_returnsNull() {
        // when no templates exist at all, should return null
        when(repository.findFirstByIsDefaultTrue()).thenReturn(Optional.empty());

        PartyDto supplier = new PartyDto();
        supplier.setDefaultTemplateId(null);

        InvoiceTemplates result = service.resolveEffectiveTemplate(supplier);

        assertThat(result).isNull();
    }

    // === resolveTemplate(id, name, supplier) ===

    @Test
    void resolveTemplate_idProvided_returnsTemplateById() {
        // explicit id wins over everything else
        InvoiceTemplates byId = createTemplate(7L, "By Id");
        when(repository.findById(7L)).thenReturn(Optional.of(byId));

        InvoiceTemplates result = service.resolveTemplate(7L, "ignored", supplierWithDefault(1L));

        assertThat(result.getName()).isEqualTo("By Id");
    }

    @Test
    void resolveTemplate_idNotFound_throws() {
        // unknown id -> IllegalArgumentException (mapped to 400 by ApiExceptionHandler)
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveTemplate(999L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("template not found: id=999");
    }

    @Test
    void resolveTemplate_nameProvided_returnsTemplateByName() {
        // name lookup is case-insensitive and wins over supplier default when id is absent
        InvoiceTemplates byName = createTemplate(3L, "Modern EN");
        when(repository.findByNameIgnoreCase("Modern EN")).thenReturn(Optional.of(byName));

        InvoiceTemplates result = service.resolveTemplate(null, "Modern EN", supplierWithDefault(1L));

        assertThat(result.getName()).isEqualTo("Modern EN");
    }

    @Test
    void resolveTemplate_nameNotFound_throws() {
        // unknown name -> IllegalArgumentException (mapped to 400 by ApiExceptionHandler)
        when(repository.findByNameIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveTemplate(null, "missing", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("template not found: missing");
    }

    @Test
    void resolveTemplate_noIdOrName_fallsBackToSupplierDefault() {
        // neither id nor name -> supplier's default template
        Parties supplier = new Parties();
        InvoiceTemplates supplierDefault = createTemplate(5L, "Supplier Default");
        supplier.setDefaultTemplateId(supplierDefault);

        InvoiceTemplates result = service.resolveTemplate(null, "  ", supplier);

        assertThat(result.getName()).isEqualTo("Supplier Default");
    }

    @Test
    void resolveTemplate_noIdOrNameOrSupplierDefault_fallsBackToGlobal() {
        // no id, no name, no supplier default -> global default
        InvoiceTemplates global = createTemplate(9L, "Global");
        when(repository.findFirstByIsDefaultTrue()).thenReturn(Optional.of(global));

        InvoiceTemplates result = service.resolveTemplate(null, null, new Parties());

        assertThat(result.getName()).isEqualTo("Global");
    }

    private Parties supplierWithDefault(Long templateId) {
        Parties supplier = new Parties();
        InvoiceTemplates def = new InvoiceTemplates();
        def.setId(templateId);
        def.setName("Ignored Supplier Default");
        supplier.setDefaultTemplateId(def);
        return supplier;
    }

    private InvoiceTemplates createTemplate(Long id, String name) {
        // build a minimal template instance for repository stubs and assertions
        InvoiceTemplates template = new InvoiceTemplates();
        template.setId(id);
        template.setName(name);
        template.setHtml("<html>" + name + "</html>");
        return template;
    }
}
