// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.config.InvoiceLocale;
import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.entity.InvoiceItems;
import com.nullinvoice.entity.InvoiceTemplates;
import com.nullinvoice.entity.Invoices;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {

    private static final String PREVIEW_ERROR_TEMPLATE = """
        <div style='padding:20px;color:#f87171;background:#1c1917;font-family:monospace;'>
        <h3>Template Preview Error</h3>
        <p>Preview is temporarily unavailable. The template will still work when generating invoices.</p>
        <details><summary>Error details</summary><pre>%s</pre></details></div>
        """;

    private final InvoiceLocale locale;
    private final InvoiceTemplateService invoiceTemplateService;

    public TemplateService(InvoiceLocale locale, InvoiceTemplateService invoiceTemplateService) {
        this.locale = locale;
        this.invoiceTemplateService = invoiceTemplateService;
    }

    public String renderInvoice(Invoices invoice) {
        InvoiceTemplates template = invoice.getTemplateId();
        if (template == null) {
            template = invoiceTemplateService.getDefaultTemplate();
        }
        String html = template.getHtml();
        if (html == null || html.isBlank()) {
            throw new IllegalStateException("Template '" + template.getName() + "' has no HTML content");
        }
        return renderInvoice(invoice, html);
    }

    /**
     * Resolves the HTML for an invoice.Returns stored HTML if available, otherwise re-renders from template
     * @param invoice
     * @return
     */
    public String resolveInvoiceHtml(Invoices invoice) {
        if (invoice.getInvoiceHtml() != null && !invoice.getInvoiceHtml().isBlank()) {
            return invoice.getInvoiceHtml();
        }
        return renderInvoice(invoice);
    }

    public String renderInvoice(Invoices invoice, String templateHtml) {
        return renderInvoice(invoice, templateHtml, locale);
    }

    public String renderInvoice(Invoices invoice, String templateHtml, InvoiceLocale invoiceLocale) {
        Map<String, String> vars = buildVariables(invoice, invoiceLocale);
        return replaceVariables(templateHtml, vars);
    }

    public String renderPreview(String templateHtml) {
        try {
            Invoices invoice = sampleInvoice();
            return renderInvoice(invoice, templateHtml);
        } catch (Exception e) {
            return String.format(PREVIEW_ERROR_TEMPLATE, escapeHtml(e.getMessage()));
        }
    }

    private Map<String, String> buildVariables(Invoices invoice, InvoiceLocale invoiceLocale) {
        Map<String, String> vars = new HashMap<>();
        String currencyCode = invoice.getCurrencyCode();

        // basic invoice info
        vars.put("invoiceNumber", escapeHtml(invoice.getInvoiceNumber()));
        vars.put("issueDate", invoiceLocale.formatDate(toLocalDate(invoice.getIssueDate())));

        // due date (conditional row)
        if (invoice.getDueDate() != null) {
            vars.put("dueDateRow", "<div><strong>Due Date:</strong> " + invoiceLocale.formatDate(toLocalDate(invoice.getDueDate())) + "</div>");
        } else {
            vars.put("dueDateRow", "");
        }

        // supplier info
        vars.put("supplierName", escapeHtml(invoice.getSupplierName()));
        vars.put("supplierAddressLine1", escapeHtml(invoice.getSupplierAddressLine1()));
        vars.put("supplierAddressLine2Row", optionalRow(invoice.getSupplierAddressLine2()));
        vars.put("supplierCityRegionPostal", buildCityRegionPostal(invoice.getSupplierCity(), invoice.getSupplierRegion(), invoice.getSupplierPostalCode()));
        vars.put("supplierCountry", escapeHtml(invoice.getSupplierCountry()));
        vars.put("supplierTaxIdRow", optionalLabeledRow("Tax ID", invoice.getSupplierTaxId()));
        vars.put("supplierVatIdRow", optionalLabeledRow("VAT ID", invoice.getSupplierVatId()));
        vars.put("supplierEmailRow", optionalLabeledRow("Email", invoice.getSupplierEmail()));
        vars.put("supplierPhoneRow", optionalLabeledRow("Phone", invoice.getSupplierPhone()));

        // client info
        vars.put("clientName", escapeHtml(invoice.getClientName()));
        vars.put("clientAddressLine1", escapeHtml(invoice.getClientAddressLine1()));
        vars.put("clientAddressLine2Row", optionalRow(invoice.getClientAddressLine2()));
        vars.put("clientCityRegionPostal", buildCityRegionPostal(invoice.getClientCity(), invoice.getClientRegion(), invoice.getClientPostalCode()));
        vars.put("clientCountry", escapeHtml(invoice.getClientCountry()));
        vars.put("clientTaxIdRow", optionalLabeledRow("Tax ID", invoice.getClientTaxId()));
        vars.put("clientVatIdRow", optionalLabeledRow("VAT ID", invoice.getClientVatId()));
        vars.put("clientEmailRow", optionalLabeledRow("Email", invoice.getClientEmail()));
        vars.put("clientPhoneRow", optionalLabeledRow("Phone", invoice.getClientPhone()));

        // items
        vars.put("itemsRows", buildItemsRows(invoice.getInvoiceItemsCollection(), currencyCode, invoiceLocale));

        // totals
        vars.put("subtotal", invoiceLocale.formatMoney(invoice.getSubtotal(), currencyCode));
        vars.put("discountTotal", invoiceLocale.formatMoney(calculateDiscountTotal(invoice.getInvoiceItemsCollection()), currencyCode));
        vars.put("taxTotal", invoiceLocale.formatMoney(invoice.getTaxTotal(), currencyCode));
        vars.put("total", invoiceLocale.formatMoney(invoice.getTotal(), currencyCode));

        // notes (conditional section)
        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            vars.put("notesSection", "<div class=\"notes\"><strong>Notes:</strong> " + escapeHtml(invoice.getNotes()) + "</div>");
        } else {
            vars.put("notesSection", "");
        }

        return vars;
    }

    private String replaceVariables(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private String buildCityRegionPostal(String city, String region, String postalCode) {
        StringBuilder sb = new StringBuilder();
        sb.append(escapeHtml(city));
        if (region != null && !region.isBlank()) {
            sb.append(", ").append(escapeHtml(region));
        }
        if (postalCode != null && !postalCode.isBlank()) {
            sb.append(" ").append(escapeHtml(postalCode));
        }
        return sb.toString();
    }

    private String optionalRow(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "<div>" + escapeHtml(value) + "</div>";
    }

    private String optionalLabeledRow(String label, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return "<div>" + label + ": " + escapeHtml(value) + "</div>";
    }

    private String buildItemsRows(Collection<InvoiceItems> items, String currencyCode, InvoiceLocale invoiceLocale) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (InvoiceItems item : items) {
            BigDecimal discount = item.getDiscount() == null ? BigDecimal.ZERO : item.getDiscount();
            BigDecimal lineTax = item.getLineTax() == null ? BigDecimal.ZERO : item.getLineTax();
            sb.append("<tr>");
            sb.append("<td>").append(item.getLineNumber()).append("</td>");
            sb.append("<td>").append(escapeHtml(item.getDescription())).append("</td>");
            sb.append("<td>").append(item.getQuantity().toPlainString()).append("</td>");
            sb.append("<td>").append(invoiceLocale.formatMoney(item.getUnitPrice(), currencyCode)).append("</td>");
            sb.append("<td>").append(invoiceLocale.formatMoney(discount, currencyCode)).append("</td>");
            sb.append("<td>").append(formatTaxRate(item.getTaxRate())).append("</td>");
            sb.append("<td>").append(invoiceLocale.formatMoney(lineTax, currencyCode)).append("</td>");
            sb.append("<td>").append(invoiceLocale.formatMoney(item.getLineTotal(), currencyCode)).append("</td>");
            sb.append("</tr>");
        }
        return sb.toString();
    }

    private String formatTaxRate(BigDecimal taxRate) {
        return taxRate.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private BigDecimal calculateDiscountTotal(Collection<InvoiceItems> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceItems item : items) {
            if (item.getDiscount() != null) {
                total = total.add(item.getDiscount());
            }
        }
        return total;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private Invoices sampleInvoice() {
        Invoices invoice = new Invoices();
        invoice.setInvoiceNumber("PREVIEW-0001");
        invoice.setIssueDate(toDate(LocalDate.now()));
        invoice.setDueDate(toDate(LocalDate.now().plusDays(14)));
        invoice.setCurrencyCode(locale.getCurrencyCode());
        applySupplier(invoice, sampleParty("Preview Supplier"));
        applyClient(invoice, sampleParty("Preview Client"));
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setTaxTotal(new BigDecimal("20.00"));
        invoice.setTotal(new BigDecimal("120.00"));
        invoice.setNotes("Thank you for your business.");

        InvoiceItems item = new InvoiceItems();
        item.setLineNumber(1);
        item.setDescription("Consulting services");
        item.setQuantity(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setTaxRate(new BigDecimal("0.20"));
        item.setLineSubtotal(new BigDecimal("100.00"));
        item.setLineTax(new BigDecimal("20.00"));
        item.setLineTotal(new BigDecimal("120.00"));
        item.setInvoiceId(invoice);
        invoice.setInvoiceItemsCollection(new ArrayList<>(List.of(item)));

        return invoice;
    }

    private PartyDto sampleParty(String name) {
        PartyDto party = new PartyDto();
        party.setName(name);
        party.setAddressLine1("1 Main Street");
        party.setCity("Sofia");
        party.setCountry("BG");
        party.setEmail("hello@example.com");
        party.setPhone("+359 555 0101");
        return party;
    }

    private void applySupplier(Invoices invoice, PartyDto dto) {
        invoice.setSupplierName(dto.getName());
        invoice.setSupplierAddressLine1(dto.getAddressLine1());
        invoice.setSupplierCity(dto.getCity());
        invoice.setSupplierCountry(dto.getCountry());
        invoice.setSupplierEmail(dto.getEmail());
        invoice.setSupplierPhone(dto.getPhone());
    }

    private void applyClient(Invoices invoice, PartyDto dto) {
        invoice.setClientName(dto.getName());
        invoice.setClientAddressLine1(dto.getAddressLine1());
        invoice.setClientCity(dto.getCity());
        invoice.setClientCountry(dto.getCountry());
        invoice.setClientEmail(dto.getEmail());
        invoice.setClientPhone(dto.getPhone());
    }

    private LocalDate toLocalDate(Date value) {
        if (value == null) {
            return null;
        }
        // handle java.sql.Date which doesn't support toInstant()
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return value.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private Date toDate(LocalDate value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
