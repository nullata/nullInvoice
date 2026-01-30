// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.dto.GenerateInvoiceItemDto;
import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.dto.InvoiceItemResponse;
import com.nullinvoice.dto.InvoicePartyDto;
import com.nullinvoice.dto.InvoiceResponse;
import com.nullinvoice.entity.InvoiceItems;
import com.nullinvoice.entity.Invoices;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    public Invoices toEntity(GenerateInvoiceRequest request) {
        Invoices invoice = new Invoices();
        invoice.setNotes(request.getNotes());

        List<InvoiceItems> items = request.getItems().stream()
                .map(this::toItemEntity)
                .collect(Collectors.toList());
        items.forEach(item -> item.setInvoiceId(invoice));
        invoice.setInvoiceItemsCollection(new ArrayList<>(items));

        return invoice;
    }

    public InvoiceResponse toResponse(Invoices invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceNumberInt(invoice.getInvoiceNumberInt())
                .issueDate(toLocalDate(invoice.getIssueDate()))
                .dueDate(toLocalDate(invoice.getDueDate()))
                .localeCode(invoice.getLocaleCode())
                .currencyCode(invoice.getCurrencyCode())
                .status(invoice.getStatus())
                .supplier(toSupplierDto(invoice))
                .client(toClientDto(invoice))
                .subtotal(invoice.getSubtotal())
                .taxTotal(invoice.getTaxTotal())
                .total(invoice.getTotal())
                .notes(invoice.getNotes())
                .items(toItemResponses(invoice.getInvoiceItemsCollection()))
                .createdAt(toOffsetDateTime(invoice.getCreatedAt()))
                .updatedAt(toOffsetDateTime(invoice.getUpdatedAt()))
                .build();
    }

    private InvoicePartyDto toSupplierDto(Invoices invoice) {
        return InvoicePartyDto.builder()
                .name(invoice.getSupplierName())
                .taxId(invoice.getSupplierTaxId())
                .vatId(invoice.getSupplierVatId())
                .addressLine1(invoice.getSupplierAddressLine1())
                .addressLine2(invoice.getSupplierAddressLine2())
                .city(invoice.getSupplierCity())
                .region(invoice.getSupplierRegion())
                .postalCode(invoice.getSupplierPostalCode())
                .country(invoice.getSupplierCountry())
                .email(invoice.getSupplierEmail())
                .phone(invoice.getSupplierPhone())
                .build();
    }

    private InvoicePartyDto toClientDto(Invoices invoice) {
        return InvoicePartyDto.builder()
                .name(invoice.getClientName())
                .taxId(invoice.getClientTaxId())
                .vatId(invoice.getClientVatId())
                .addressLine1(invoice.getClientAddressLine1())
                .addressLine2(invoice.getClientAddressLine2())
                .city(invoice.getClientCity())
                .region(invoice.getClientRegion())
                .postalCode(invoice.getClientPostalCode())
                .country(invoice.getClientCountry())
                .email(invoice.getClientEmail())
                .phone(invoice.getClientPhone())
                .build();
    }

    private InvoiceItems toItemEntity(GenerateInvoiceItemDto dto) {
        InvoiceItems item = new InvoiceItems();
        item.setDescription(dto.getDescription());
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice());
        item.setDiscount(dto.getDiscount());
        item.setTaxRate(dto.getTaxRate());
        item.setCreatedAt(new Date());
        return item;
    }

    private List<InvoiceItemResponse> toItemResponses(java.util.Collection<InvoiceItems> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream().map(this::toItemResponse).collect(Collectors.toList());
    }

    private InvoiceItemResponse toItemResponse(InvoiceItems item) {
        return InvoiceItemResponse.builder()
                .lineNumber(item.getLineNumber())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discount(item.getDiscount())
                .taxRate(item.getTaxRate())
                .lineSubtotal(item.getLineSubtotal())
                .lineTax(item.getLineTax())
                .lineTotal(item.getLineTotal())
                .build();
    }

    /**
     * Converts a Date to LocalDate
     *
     * @param value
     * @return
     */
    public LocalDate toLocalDate(Date value) {
        if (value == null) {
            return null;
        }
        // handle java.sql.Date which doesn't support toInstant()
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return value.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private OffsetDateTime toOffsetDateTime(Date value) {
        if (value == null) {
            return null;
        }
        return value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
