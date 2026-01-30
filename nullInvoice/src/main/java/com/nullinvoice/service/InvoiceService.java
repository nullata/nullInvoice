// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.config.InvoiceLocale;
import com.nullinvoice.config.SupplierLocaleFactory;
import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.dto.InvoicePartyDto;
import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.entity.InvoiceItems;
import com.nullinvoice.entity.InvoiceTemplates;
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.error.InvoiceNotFoundException;
import com.nullinvoice.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final String STATUS_ISSUED = "issued";
    public static final String STATUS_UNPAID = "unpaid";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLocale defaultLocale;
    private final SupplierLocaleFactory supplierLocaleFactory;
    private final InvoiceMapper mapper;
    private final PartyProfileService partyProfileService;
    private final TemplateService templateService;
    private final InvoiceTemplateService invoiceTemplateService;

    @Transactional
    public Invoices generateInvoice(GenerateInvoiceRequest request) {
        return generateInvoice(request, false);
    }

    @Transactional
    public Invoices generateInvoice(GenerateInvoiceRequest request, boolean markUnpaid) {
        Invoices invoice = mapper.toEntity(request);
        LocalDate issueDate = request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now(ZoneOffset.UTC);
        invoice.setIssueDate(toDate(issueDate));
        invoice.setDueDate(toDate(request.getDueDate()));
        invoice.setStatus(resolveInitialStatus(request, markUnpaid));

        // lookup and lock supplier atomically to prevent race conditions on invoice number
        Parties supplier = partyProfileService.lockSupplierForUpdate(request.getSupplierId());
        applySupplierFromParty(invoice, supplier);
        invoice.setSupplierPartyId(supplier);

        // client: lookup by id or upsert from details
        Parties client;
        InvoicePartyDto clientDto = request.getClient();
        if (clientDto.getId() != null) {
            client = partyProfileService.findClientEntityById(clientDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("client not found"));
            applyClientFromParty(invoice, client);
        } else {
            client = partyProfileService.upsertParty(toPartyDto(clientDto), PartyProfileService.ROLE_CLIENT);
            applyClientFromParty(invoice, client);
        }
        invoice.setClientPartyId(client);

        // create locale from supplier settings
        InvoiceLocale supplierLocale = supplierLocaleFactory.createFromSupplier(supplier);
        invoice.setLocaleCode(supplierLocale.getCode());
        invoice.setCurrencyCode(resolveCurrencyCode(request.getCurrencyCode(), supplierLocale));

        // apply supplier's default tax rate to items without one
        BigDecimal defaultTaxRate = supplier.getDefaultTaxRate() != null ? supplier.getDefaultTaxRate() : ZERO;
        applyTotalsAndLines(invoice, defaultTaxRate);

        // calculate next invoice number
        long nextNumber = calculateNextInvoiceNumber(supplier);
        String supplierPrefix = supplierLocaleFactory.getInvoicePrefix(supplier);
        int numberDigits = supplierLocaleFactory.getInvoiceNumberDigits(supplier);

        invoice.setInvoiceNumberInt(nextNumber);
        invoice.setInvoiceNumber(formatInvoiceNumber(supplierPrefix, nextNumber, numberDigits));

        InvoiceTemplates template = invoiceTemplateService.resolveTemplate(null, supplier);
        invoice.setTemplateId(template);
        invoice.setInvoiceHtml(templateService.renderInvoice(invoice, template.getHtml(), supplierLocale));

        Date now = new Date();
        invoice.setCreatedAt(now);
        invoice.setUpdatedAt(now);

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoices markInvoiceIssued(String invoiceNumber, Long supplierId) {
        Invoices invoice = getInvoiceByNumber(invoiceNumber, supplierId);
        String currentStatus = invoice.getStatus();
        if (currentStatus == null || STATUS_ISSUED.equalsIgnoreCase(currentStatus)) {
            return invoice;
        }
        if (!STATUS_UNPAID.equalsIgnoreCase(currentStatus)) {
            throw new IllegalArgumentException("invoice status cannot be updated");
        }
        invoice.setStatus(STATUS_ISSUED);
        invoice.setUpdatedAt(new Date());
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public java.util.List<Invoices> listInvoicesByStatus(String status) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (status == null || status.isBlank()) {
            return invoiceRepository.findAll(sort);
        }
        String normalized = normalizeStatus(status);
        return invoiceRepository.findAllByStatusIgnoreCase(normalized, sort);
    }

    private long calculateNextInvoiceNumber(Parties supplier) {
        long startNumber = supplierLocaleFactory.getInvoiceStartNumber(supplier);
        Long lastNumber = supplier.getLastInvoiceNumber();
        long baseline = lastNumber == null
                ? invoiceRepository.findMaxInvoiceNumberIntBySupplierId(supplier.getId())
                : lastNumber;
        long nextNumber = Math.max(startNumber, baseline + 1);
        supplier.setLastInvoiceNumber(nextNumber);
        return nextNumber;
    }

    @Transactional(readOnly = true)
    public Invoices getInvoiceByNumber(String invoiceNumber) {
        return getInvoiceByNumber(invoiceNumber, null);
    }

    @Transactional(readOnly = true)
    public Invoices getInvoiceByNumber(String invoiceNumber, Long supplierId) {
        if (supplierId != null) {
            return invoiceRepository.findWithItemsByInvoiceNumberAndSupplierPartyId_Id(invoiceNumber, supplierId)
                    .orElseThrow(() -> new InvoiceNotFoundException(invoiceNumber));
        }
        if (invoiceRepository.countByInvoiceNumber(invoiceNumber) > 1) {
            throw new IllegalArgumentException("invoice number is not unique; provide supplier_id");
        }
        return invoiceRepository.findWithItemsByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceNumber));
    }

    @Transactional(readOnly = true)
    public Page<Invoices> listInvoices(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Invoices> searchInvoices(String type, String query, Pageable pageable) {
        return searchInvoices(type, query, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Invoices> searchInvoices(String type, String query, Long supplierId, Pageable pageable) {
        String trimmedQuery = query == null ? "" : query.trim();
        boolean hasQuery = !trimmedQuery.isEmpty();
        boolean hasSupplier = supplierId != null;

        if (!hasQuery && !hasSupplier) {
            return listInvoices(pageable);
        }

        if (!hasQuery && hasSupplier) {
            return invoiceRepository.findAllBySupplierPartyId_Id(supplierId, pageable);
        }

        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);

        if (hasSupplier) {
            switch (normalizedType) {
                case "date": {
                    Date issueDate = parseIssueDate(trimmedQuery);
                    if (issueDate == null) {
                        return Page.empty(pageable);
                    }
                    return invoiceRepository.findAllBySupplierPartyId_IdAndIssueDate(supplierId, issueDate, pageable);
                }
                case "client":
                    return invoiceRepository.findAllBySupplierPartyId_IdAndClientNameContainingIgnoreCase(supplierId, trimmedQuery, pageable);
                case "supplier":
                    return invoiceRepository.findAllBySupplierPartyId_Id(supplierId, pageable);
                case "number":
                default:
                    return invoiceRepository.findAllBySupplierPartyId_IdAndInvoiceNumberContainingIgnoreCase(supplierId, trimmedQuery, pageable);
            }
        }

        switch (normalizedType) {
            case "date": {
                Date issueDate = parseIssueDate(trimmedQuery);
                if (issueDate == null) {
                    return Page.empty(pageable);
                }
                return invoiceRepository.findAllByIssueDate(issueDate, pageable);
            }
            case "client":
                return invoiceRepository.findAllByClientNameContainingIgnoreCase(trimmedQuery, pageable);
            case "supplier":
                return invoiceRepository.findAllBySupplierNameContainingIgnoreCase(trimmedQuery, pageable);
            case "number":
            default:
                return invoiceRepository.findAllByInvoiceNumberContainingIgnoreCase(trimmedQuery, pageable);
        }
    }

    @Transactional(readOnly = true)
    public long countInvoices() {
        return invoiceRepository.count();
    }

    private void applyTotalsAndLines(Invoices invoice, BigDecimal defaultTaxRate) {
        BigDecimal subtotal = ZERO;
        BigDecimal taxTotal = ZERO;
        BigDecimal total = ZERO;

        Collection<InvoiceItems> items = invoice.getInvoiceItemsCollection();
        if (items == null) {
            items = java.util.List.of();
        }
        int lineNumber = 1;
        Date now = new Date();
        for (InvoiceItems item : items) {
            item.setLineNumber(lineNumber++);
            BigDecimal lineSubtotal = scaleMoney(item.getQuantity().multiply(item.getUnitPrice()));
            BigDecimal discount = scaleMoney(item.getDiscount() == null ? ZERO : item.getDiscount());
            BigDecimal discountedSubtotal = lineSubtotal.subtract(discount);
            if (discountedSubtotal.compareTo(ZERO) < 0) {
                discountedSubtotal = ZERO;
            }
            BigDecimal taxRate = item.getTaxRate() != null ? item.getTaxRate() : defaultTaxRate;
            item.setTaxRate(taxRate);
            BigDecimal lineTax = scaleMoney(discountedSubtotal.multiply(taxRate));
            BigDecimal lineTotal = scaleMoney(discountedSubtotal.add(lineTax));

            item.setDiscount(discount);
            item.setLineSubtotal(discountedSubtotal);
            item.setLineTax(lineTax);
            item.setLineTotal(lineTotal);
            if (item.getCreatedAt() == null) {
                item.setCreatedAt(now);
            }

            subtotal = subtotal.add(discountedSubtotal);
            taxTotal = taxTotal.add(lineTax);
            total = total.add(lineTotal);
        }

        invoice.setSubtotal(scaleMoney(subtotal));
        invoice.setTaxTotal(scaleMoney(taxTotal));
        invoice.setTotal(scaleMoney(total));
    }

    private String resolveInitialStatus(GenerateInvoiceRequest request, boolean markUnpaid) {
        if (!markUnpaid) {
            return STATUS_ISSUED;
        }
        if (request.getDueDate() == null) {
            throw new IllegalArgumentException("due_date is required to mark invoice unpaid");
        }
        return STATUS_UNPAID;
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (STATUS_ISSUED.equals(normalized) || STATUS_UNPAID.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("invalid status: " + normalized);
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String formatInvoiceNumber(String prefix, long number, int digits) {
        String formatted = String.format("%0" + digits + "d", number);
        if (prefix == null || prefix.isEmpty()) {
            return formatted;
        }
        return prefix + formatted;
    }

    private String resolveCurrencyCode(String currencyCode, InvoiceLocale locale) {
        String resolved = currencyCode;
        if (resolved == null || resolved.isBlank()) {
            resolved = locale.getCurrencyCode();
        }
        resolved = resolved.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(resolved);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("invalid currency_code: " + resolved);
        }
        return resolved;
    }

    private Date parseIssueDate(String input) {
        try {
            LocalDate parsed = LocalDate.parse(input);
            return toDate(parsed);
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse date '{}' as ISO format, trying dd.MM.yyyy", input);
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate parsed = LocalDate.parse(input, formatter);
            return toDate(parsed);
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse date '{}' in any supported format", input);
            return null;
        }
    }

    private Date toDate(LocalDate value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private void applySupplierFromParty(Invoices invoice, Parties supplier) {
        invoice.setSupplierName(supplier.getName());
        invoice.setSupplierTaxId(supplier.getTaxId());
        invoice.setSupplierVatId(supplier.getVatId());
        invoice.setSupplierAddressLine1(supplier.getAddressLine1());
        invoice.setSupplierAddressLine2(supplier.getAddressLine2());
        invoice.setSupplierCity(supplier.getCity());
        invoice.setSupplierRegion(supplier.getRegion());
        invoice.setSupplierPostalCode(supplier.getPostalCode());
        invoice.setSupplierCountry(supplier.getCountry());
        invoice.setSupplierEmail(supplier.getEmail());
        invoice.setSupplierPhone(supplier.getPhone());
    }

    private void applyClientFromParty(Invoices invoice, Parties client) {
        invoice.setClientName(client.getName());
        invoice.setClientTaxId(client.getTaxId());
        invoice.setClientVatId(client.getVatId());
        invoice.setClientAddressLine1(client.getAddressLine1());
        invoice.setClientAddressLine2(client.getAddressLine2());
        invoice.setClientCity(client.getCity());
        invoice.setClientRegion(client.getRegion());
        invoice.setClientPostalCode(client.getPostalCode());
        invoice.setClientCountry(client.getCountry());
        invoice.setClientEmail(client.getEmail());
        invoice.setClientPhone(client.getPhone());
    }

    private PartyDto toPartyDto(InvoicePartyDto client) {
        PartyDto dto = new PartyDto();
        dto.setName(client.getName());
        dto.setTaxId(client.getTaxId());
        dto.setVatId(client.getVatId());
        dto.setAddressLine1(client.getAddressLine1());
        dto.setAddressLine2(client.getAddressLine2());
        dto.setCity(client.getCity());
        dto.setRegion(client.getRegion());
        dto.setPostalCode(client.getPostalCode());
        dto.setCountry(client.getCountry());
        dto.setEmail(client.getEmail());
        dto.setPhone(client.getPhone());
        return dto;
    }

    /**
     * Formats the invoice filename based on supplier name and invoice number
     *
     * @param invoice
     * @return
     */
    public String formatInvoiceFilename(Invoices invoice) {
        String supplierName = normalizeFilenameSegment(invoice.getSupplierName());
        String prefix = supplierName.isEmpty() ? "invoice" : supplierName;
        return prefix + "_" + invoice.getInvoiceNumber() + ".pdf";
    }

    private String normalizeFilenameSegment(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", "-");
    }

    /**
     * Calculates the total discount across all invoice items
     *
     * @param items
     * @return
     */
    public BigDecimal calculateDiscountTotal(java.util.Collection<InvoiceItems> items) {
        if (items == null || items.isEmpty()) {
            return ZERO;
        }
        return items.stream()
                .map(item -> item.getDiscount() == null ? ZERO : item.getDiscount())
                .reduce(ZERO, BigDecimal::add);
    }

    /**
     * Finds invoices for a supplier within a date range
     * @param supplierId
     * @param startDate
     * @param endDate
     * @return 
     */
    @Transactional(readOnly = true)
    public List<Invoices> findInvoicesByDateRange(Long supplierId, Date startDate, Date endDate) {
        Sort sort = Sort.by(Sort.Direction.ASC, "issueDate", "invoiceNumber");
        return invoiceRepository.findAllBySupplierPartyId_IdAndIssueDateBetween(supplierId, startDate, endDate, sort);
    }

    /**
     * Counts invoices for a supplier within a date range
     * @param supplierId
     * @param startDate
     * @param endDate
     * @return 
     */
    @Transactional(readOnly = true)
    public long countInvoicesByDateRange(Long supplierId, Date startDate, Date endDate) {
        return invoiceRepository.countBySupplierPartyId_IdAndIssueDateBetween(supplierId, startDate, endDate);
    }

    /**
     * Gets the earliest invoice date for a supplier
     * @param supplierId
     * @return 
     */
    @Transactional(readOnly = true)
    public Date getEarliestDate(Long supplierId) {
        return invoiceRepository.findEarliestIssueDateBySupplierId(supplierId);
    }

    /**
     * Gets the latest invoice date for a supplier
     * @param supplierId
     * @return 
     */
    @Transactional(readOnly = true)
    public Date getLatestDate(Long supplierId) {
        return invoiceRepository.findLatestIssueDateBySupplierId(supplierId);
    }
}
