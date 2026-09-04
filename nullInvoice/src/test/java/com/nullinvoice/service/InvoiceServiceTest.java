// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.config.InvoiceLocale;
import com.nullinvoice.config.SupplierLocaleFactory;
import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.dto.InvoicePartyDto;
import com.nullinvoice.entity.InvoiceItems;
import com.nullinvoice.entity.InvoiceTemplates;
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for InvoiceService utility methods: filename formatting and
 * discount calculation
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceLocale defaultLocale;

    @Mock
    private SupplierLocaleFactory supplierLocaleFactory;

    @Mock
    private InvoiceMapper mapper;

    @Mock
    private PartyProfileService partyProfileService;

    @Mock
    private TemplateService templateService;

    @Mock
    private InvoiceTemplateService invoiceTemplateService;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
                invoiceRepository,
                defaultLocale,
                supplierLocaleFactory,
                mapper,
                partyProfileService,
                templateService,
                invoiceTemplateService
        );
    }

    // === formatInvoiceFilename ===
    @Test
    void formatInvoiceFilename_withSupplierName_usesSupplierNameAsPrefix() {
        // supplier name should be used as filename prefix with spaces replaced by dashes
        Invoices invoice = new Invoices();
        invoice.setSupplierName("Acme Corp");
        invoice.setInvoiceNumber("INV-001");

        String filename = invoiceService.formatInvoiceFilename(invoice);

        assertThat(filename).isEqualTo("Acme-Corp_INV-001.pdf");
    }

    @Test
    void formatInvoiceFilename_withSpacesInSupplierName_replacesSpacesWithDashes() {
        // multiple consecutive spaces should collapse to a single dash
        Invoices invoice = new Invoices();
        invoice.setSupplierName("Acme   Multi  Space   Corp");
        invoice.setInvoiceNumber("INV-002");

        String filename = invoiceService.formatInvoiceFilename(invoice);

        assertThat(filename).isEqualTo("Acme-Multi-Space-Corp_INV-002.pdf");
    }

    @Test
    void formatInvoiceFilename_withNullSupplierName_usesInvoiceAsPrefix() {
        // missing supplier name should fall back to generic "invoice" prefix
        Invoices invoice = new Invoices();
        invoice.setSupplierName(null);
        invoice.setInvoiceNumber("INV-003");

        String filename = invoiceService.formatInvoiceFilename(invoice);

        assertThat(filename).isEqualTo("invoice_INV-003.pdf");
    }

    @Test
    void formatInvoiceFilename_withEmptySupplierName_usesInvoiceAsPrefix() {
        // blank supplier name should fall back to generic "invoice" prefix
        Invoices invoice = new Invoices();
        invoice.setSupplierName("   ");
        invoice.setInvoiceNumber("INV-004");

        String filename = invoiceService.formatInvoiceFilename(invoice);

        assertThat(filename).isEqualTo("invoice_INV-004.pdf");
    }

    @Test
    void formatInvoiceFilename_withLeadingTrailingSpaces_trimsThem() {
        // leading and trailing whitespace should be trimmed from supplier name
        Invoices invoice = new Invoices();
        invoice.setSupplierName("  Trimmed Name  ");
        invoice.setInvoiceNumber("INV-005");

        String filename = invoiceService.formatInvoiceFilename(invoice);

        assertThat(filename).isEqualTo("Trimmed-Name_INV-005.pdf");
    }

    // === calculateDiscountTotal ===
    @Test
    void calculateDiscountTotal_withMultipleDiscounts_sumsAll() {
        // all item discounts should be summed together
        List<InvoiceItems> items = List.of(
                createItemWithDiscount(new BigDecimal("10.00")),
                createItemWithDiscount(new BigDecimal("5.50")),
                createItemWithDiscount(new BigDecimal("2.25"))
        );

        BigDecimal total = invoiceService.calculateDiscountTotal(items);

        assertThat(total).isEqualByComparingTo("17.75");
    }

    @Test
    void calculateDiscountTotal_withNullDiscounts_treatsAsZero() {
        // null discounts on individual items should be treated as zero
        List<InvoiceItems> items = List.of(
                createItemWithDiscount(new BigDecimal("10.00")),
                createItemWithDiscount(null),
                createItemWithDiscount(new BigDecimal("5.00"))
        );

        BigDecimal total = invoiceService.calculateDiscountTotal(items);

        assertThat(total).isEqualByComparingTo("15.00");
    }

    @Test
    void calculateDiscountTotal_withAllNullDiscounts_returnsZero() {
        // all discounts are null, so total should be zero
        List<InvoiceItems> items = List.of(
                createItemWithDiscount(null),
                createItemWithDiscount(null)
        );

        BigDecimal total = invoiceService.calculateDiscountTotal(items);

        assertThat(total).isEqualByComparingTo("0");
    }

    @Test
    void calculateDiscountTotal_withEmptyList_returnsZero() {
        // empty item list should return zero discount
        BigDecimal total = invoiceService.calculateDiscountTotal(new ArrayList<>());

        assertThat(total).isEqualByComparingTo("0");
    }

    @Test
    void calculateDiscountTotal_withNullList_returnsZero() {
        // null item list should return zero discount without throwing
        BigDecimal total = invoiceService.calculateDiscountTotal(null);

        assertThat(total).isEqualByComparingTo("0");
    }

    @Test
    void calculateDiscountTotal_withSingleItem_returnsItemDiscount() {
        // single item discount should be returned directly
        List<InvoiceItems> items = List.of(createItemWithDiscount(new BigDecimal("25.99")));

        BigDecimal total = invoiceService.calculateDiscountTotal(items);

        assertThat(total).isEqualByComparingTo("25.99");
    }

    // === markInvoiceIssued ===
    @Test
    void markInvoiceIssued_withUnpaidStatus_updatesToIssued() {
        Invoices invoice = new Invoices();
        invoice.setStatus(InvoiceService.STATUS_UNPAID);
        when(invoiceRepository.findWithItemsByInvoiceNumberAndSupplierPartyId_Id("INV-100", 1L))
                .thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Invoices updated = invoiceService.markInvoiceIssued("INV-100", 1L);

        assertThat(updated.getStatus()).isEqualTo(InvoiceService.STATUS_ISSUED);
    }

    @Test
    void markInvoiceIssued_alreadyIssued_isNoOp() {
        Invoices invoice = new Invoices();
        invoice.setStatus(InvoiceService.STATUS_ISSUED);
        when(invoiceRepository.findWithItemsByInvoiceNumberAndSupplierPartyId_Id("INV-101", 1L))
                .thenReturn(Optional.of(invoice));

        Invoices updated = invoiceService.markInvoiceIssued("INV-101", 1L);

        assertThat(updated.getStatus()).isEqualTo(InvoiceService.STATUS_ISSUED);
        verify(invoiceRepository, never()).save(any());
    }

    // === generateInvoice template selection wiring ===
    @Test
    void generateInvoice_passesRequestTemplateIdAndNameToResolver() {
        // Verifies InvoiceService forwards the request's templateId/templateName to
        // InvoiceTemplateService.resolveTemplate(id, name, supplier). Guards against silent
        // wiring regressions (e.g. someone reverting to resolveTemplate(null, supplier)).
        GenerateInvoiceRequest request = new GenerateInvoiceRequest();
        request.setSupplierId(1L);
        request.setCurrencyCode("EUR");
        request.setTemplateId(77L);
        request.setTemplateName("named");
        request.setClient(InvoicePartyDto.builder().id(9L).build());
        request.setItems(List.of()); // items collection is on the entity, not the request, for this path

        Parties supplier = new Parties();
        supplier.setId(1L);
        Parties client = new Parties();
        client.setId(9L);
        InvoiceTemplates resolved = new InvoiceTemplates();
        resolved.setName("resolved");
        resolved.setHtml("<html/>");

        // stub just enough of generateInternal to reach the resolveTemplate call and the save
        when(mapper.toEntity(request)).thenReturn(new Invoices());
        when(partyProfileService.lockSupplierForUpdate(1L)).thenReturn(supplier);
        when(partyProfileService.findClientEntityById(9L)).thenReturn(Optional.of(client));
        when(supplierLocaleFactory.createFromSupplier(supplier)).thenReturn(defaultLocale);
        when(defaultLocale.getCode()).thenReturn("en_US");
        when(supplierLocaleFactory.getInvoicePrefix(supplier)).thenReturn("INV");
        when(supplierLocaleFactory.getInvoiceNumberDigits(supplier)).thenReturn(5);
        when(supplierLocaleFactory.getInvoiceStartNumber(supplier)).thenReturn(1L);
        when(invoiceRepository.findMaxInvoiceNumberIntBySupplierId(1L)).thenReturn(0L);
        when(invoiceTemplateService.resolveTemplate(77L, "named", supplier)).thenReturn(resolved);
        when(templateService.renderInvoice(any(Invoices.class), eq("<html/>"), eq(defaultLocale))).thenReturn("<html>rendered</html>");
        when(invoiceRepository.save(any(Invoices.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceService.generateInvoice(request);

        // the resolver must receive the exact id/name/supplier triple, not (null, supplier)
        verify(invoiceTemplateService).resolveTemplate(77L, "named", supplier);
    }

    private InvoiceItems createItemWithDiscount(BigDecimal discount) {
        InvoiceItems item = new InvoiceItems();
        item.setDiscount(discount);
        return item;
    }
}
