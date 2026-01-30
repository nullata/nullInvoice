// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.config.InvoiceLocale;
import com.nullinvoice.entity.InvoiceTemplates;
import com.nullinvoice.entity.Invoices;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for TemplateService HTML resolution logic
 */
@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private InvoiceLocale locale;

    @Mock
    private InvoiceTemplateService invoiceTemplateService;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(locale, invoiceTemplateService);
    }

    // === resolveInvoiceHtml ===
    @Test
    void resolveInvoiceHtml_withStoredHtml_returnsStoredHtml() {
        // when invoice has pre-rendered HTML, it should be returned directly
        Invoices invoice = new Invoices();
        invoice.setInvoiceHtml("<html>Stored HTML</html>");

        String result = templateService.resolveInvoiceHtml(invoice);

        assertThat(result).isEqualTo("<html>Stored HTML</html>");
    }

    @Test
    void resolveInvoiceHtml_withNullStoredHtml_rendersFromTemplate() {
        // missing HTML should trigger re-rendering from the invoice's template
        Invoices invoice = createInvoiceWithTemplate();
        invoice.setInvoiceHtml(null);

        String result = templateService.resolveInvoiceHtml(invoice);

        assertThat(result).contains("Test Template");
    }

    @Test
    void resolveInvoiceHtml_withEmptyStoredHtml_rendersFromTemplate() {
        // empty HTML string should trigger re-rendering
        Invoices invoice = createInvoiceWithTemplate();
        invoice.setInvoiceHtml("");

        String result = templateService.resolveInvoiceHtml(invoice);

        assertThat(result).contains("Test Template");
    }

    @Test
    void resolveInvoiceHtml_withBlankStoredHtml_rendersFromTemplate() {
        // whitespace-only HTML should trigger re-rendering
        Invoices invoice = createInvoiceWithTemplate();
        invoice.setInvoiceHtml("   ");

        String result = templateService.resolveInvoiceHtml(invoice);

        assertThat(result).contains("Test Template");
    }

    @Test
    void resolveInvoiceHtml_withStoredHtml_doesNotCallTemplateService() {
        // stored HTML should be returned without accessing the template service
        Invoices invoice = new Invoices();
        invoice.setInvoiceHtml("<html>Already rendered</html>");

        templateService.resolveInvoiceHtml(invoice);

        verify(invoiceTemplateService, never()).getDefaultTemplate();
    }

    private Invoices createInvoiceWithTemplate() {
        // minimal invoice with a template and number for rendering assertions
        InvoiceTemplates template = new InvoiceTemplates();
        template.setName("Test Template");
        template.setHtml("<html>Test Template {{invoiceNumber}}</html>");

        Invoices invoice = new Invoices();
        invoice.setTemplateId(template);
        invoice.setInvoiceNumber("INV-001");
        invoice.setCurrencyCode("EUR");
        return invoice;
    }
}
