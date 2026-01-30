// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.DashboardStats;
import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.dto.TemplateIndicator;
import com.nullinvoice.entity.InvoiceItems;
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.service.DashboardService;
import com.nullinvoice.service.InvoiceService;
import com.nullinvoice.service.InvoiceTemplateService;
import com.nullinvoice.service.PartyProfileService;
import com.nullinvoice.service.SearchParameterService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class UiController {

    private static final String COOKIE_SUPPLIER_ID = "selectedSupplierId";
    private static final String COOKIE_THEME = "theme";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 365; // 1 year

    private final PartyProfileService partyProfileService;
    private final InvoiceTemplateService invoiceTemplateService;
    private final InvoiceService invoiceService;
    private final DashboardService dashboardService;
    private final SearchParameterService searchParameterService;

    // === DASHBOARD ===
    @GetMapping("/")
    public String dashboard(
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            Model model) {
        DashboardStats stats = dashboardService.getStats();
        model.addAttribute("stats", stats);
        addNavAttributes(model, supplierId);
        return "dashboard";
    }

    // === INVOICES LIST ===
    @GetMapping("/invoices")
    public String listInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "type", defaultValue = "number") String searchType,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(name = "supplier", required = false) Long filterSupplierId,
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long cookieSupplierId,
            Model model) {
        String normalizedSortBy = searchParameterService.normalizeSortBy(sortBy);
        String normalizedSortDir = searchParameterService.normalizeSortDir(sortDir);
        Pageable pageable = searchParameterService.buildPageable(page, size, normalizedSortBy, normalizedSortDir);
        String normalizedType = searchParameterService.normalizeSearchType(searchType);
        Page<Invoices> invoices = invoiceService.searchInvoices(normalizedType, query, filterSupplierId, pageable);

        model.addAttribute("invoices", invoices);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", invoices.getTotalPages());
        model.addAttribute("searchQuery", searchParameterService.normalizeSearchQuery(normalizedType, query));
        model.addAttribute("searchType", normalizedType);
        model.addAttribute("sortBy", normalizedSortBy);
        model.addAttribute("sortDir", normalizedSortDir);
        model.addAttribute("filterSupplierId", filterSupplierId);
        model.addAttribute("suppliers", partyProfileService.listSuppliers());
        addNavAttributes(model, cookieSupplierId);

        return "invoices";
    }

    // === INVOICE DETAILS ===
    @GetMapping("/invoices/{invoiceNumber}")
    public String viewInvoice(
            @PathVariable String invoiceNumber,
            @RequestParam(name = "supplierId", required = false) Long supplierIdParam,
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            Model model) {
        Long effectiveSupplierId = supplierIdParam != null ? supplierIdParam : supplierId;
        Invoices invoice = invoiceService.getInvoiceByNumber(invoiceNumber, effectiveSupplierId);
        List<InvoiceItems> items = new ArrayList<>();
        if (invoice.getInvoiceItemsCollection() != null) {
            items.addAll(invoice.getInvoiceItemsCollection());
            items.sort(Comparator.comparingInt(InvoiceItems::getLineNumber));
        }

        model.addAttribute("invoice", invoice);
        model.addAttribute("items", items);
        model.addAttribute("discountTotal", invoiceService.calculateDiscountTotal(items));
        model.addAttribute("supplierId", effectiveSupplierId);
        addNavAttributes(model, supplierId);

        return "invoice-details";
    }

    // === MARK INVOICE AS PAID (issued) ===
    @PostMapping("/invoices/{invoiceNumber}/mark-issued")
    public String markInvoiceIssued(
            @PathVariable String invoiceNumber,
            @RequestParam(name = "supplierId", required = false) Long supplierId) {
        invoiceService.markInvoiceIssued(invoiceNumber, supplierId);
        String supplierQuery = supplierId != null ? "?supplierId=" + supplierId : "";
        return "redirect:/invoices/" + invoiceNumber + supplierQuery;
    }

    // === NEW INVOICE ===
    @GetMapping("/invoices/new")
    public String newInvoice(
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            @CookieValue(name = COOKIE_THEME, required = false) String theme,
            Model model) {
        PartyDto supplier = resolveSupplier(supplierId);
        model.addAttribute("supplier", supplier);
        model.addAttribute("supplierMissing", supplier == null);

        // check template availability for warning modals
        boolean hasAnyTemplates = !invoiceTemplateService.listTemplates().isEmpty();
        boolean hasDefaultTemplate = invoiceTemplateService.resolveEffectiveTemplate(supplier) != null;

        model.addAttribute("templatesMissing", !hasAnyTemplates);
        model.addAttribute("defaultTemplateMissing", hasAnyTemplates && !hasDefaultTemplate);
        model.addAttribute("theme", theme != null ? theme : "dark");
        model.addAttribute("defaultTaxRate", supplier != null ? supplier.getDefaultTaxRate() : null);
        addNavAttributes(model, supplierId);

        return "invoice-form";
    }

    // === SUPPLIERS LIST ===
    @GetMapping("/suppliers")
    public String listSuppliers(
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            @RequestParam(value = "saved", required = false) String saved,
            @RequestParam(value = "deleted", required = false) String deleted,
            @RequestParam(name = "q", required = false) String query,
            Model model) {
        String searchQuery = query == null ? "" : query.trim();
        if (searchQuery.isEmpty()) {
            model.addAttribute("suppliers", partyProfileService.listSuppliers());
        } else {
            model.addAttribute("suppliers", partyProfileService.searchSuppliers(searchQuery));
        }
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("selectedSupplierId", supplierId);
        model.addAttribute("saved", saved != null);
        model.addAttribute("deleted", deleted != null);
        addNavAttributes(model, supplierId);

        return "suppliers";
    }

    // === NEW SUPPLIER ===
    @GetMapping("/suppliers/new")
    public String newSupplier(
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            Model model) {
        model.addAttribute("supplier", new PartyDto());
        model.addAttribute("isNew", true);
        addNavAttributes(model, supplierId);

        return "supplier";
    }

    // === EDIT SUPPLIER ===
    @GetMapping("/suppliers/{id}")
    public String editSupplier(
            @PathVariable Long id,
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long selectedSupplierId,
            Model model) {
        PartyDto supplier = partyProfileService.findSupplierById(id).orElseGet(PartyDto::new);
        model.addAttribute("supplier", supplier);
        model.addAttribute("isNew", false);
        addTemplateIndicatorAttributes(model, supplier);
        addNavAttributes(model, selectedSupplierId);

        return "supplier";
    }

    // === SAVE SUPPLIER ===
    @PostMapping("/suppliers")
    public String saveSupplier(
            @Valid @ModelAttribute("supplier") PartyDto supplier,
            BindingResult bindingResult,
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long selectedSupplierId,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", supplier.getId() == null);
            addNavAttributes(model, selectedSupplierId);
            return "supplier";
        }

        try {
            partyProfileService.saveSupplierProfile(supplier.getId(), supplier);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("isNew", supplier.getId() == null);
            addNavAttributes(model, selectedSupplierId);
            return "supplier";
        }

        redirectAttributes.addAttribute("saved", "1");
        return "redirect:/suppliers";
    }

    // === SELECT SUPPLIER (set cookie) ===
    @PostMapping("/suppliers/{id}/select")
    public String selectSupplier(
            @PathVariable Long id,
            HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_SUPPLIER_ID, String.valueOf(id));
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        response.addCookie(cookie);

        return "redirect:/suppliers";
    }

    // === DELETE SUPPLIER (soft delete) ===
    @PostMapping("/suppliers/{id}/delete")
    public String deleteSupplier(
            @PathVariable Long id,
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long selectedSupplierId,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        partyProfileService.deleteSupplier(id);

        // if deleted supplier was selected, clear the cookie
        if (id.equals(selectedSupplierId)) {
            Cookie cookie = new Cookie(COOKIE_SUPPLIER_ID, "");
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }

        redirectAttributes.addAttribute("deleted", "1");
        return "redirect:/suppliers";
    }

    // === GET SUPPLIER INVOICE COUNT (for delete confirmation) ===
    @GetMapping("/suppliers/{id}/invoice-count")
    @ResponseBody
    public ResponseEntity<Long> getSupplierInvoiceCount(@PathVariable Long id) {
        return ResponseEntity.ok(partyProfileService.getSupplierInvoiceCount(id));
    }

    // === LEGACY SUPPLIER ENDPOINT (for backwards compat) ===
    @GetMapping("/supplier")
    public String legacySupplierForm(
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            @RequestParam(value = "saved", required = false) String saved,
            @RequestParam(value = "error", required = false) String error,
            Model model) {
        PartyDto supplier = partyProfileService.findSupplier().orElseGet(PartyDto::new);
        model.addAttribute("supplier", supplier);
        model.addAttribute("saved", saved != null);
        model.addAttribute("error", error);
        model.addAttribute("isNew", supplier.getId() == null);
        addTemplateIndicatorAttributes(model, supplier);
        addNavAttributes(model, supplierId);

        return "supplier";
    }

    @PostMapping("/supplier")
    public String legacySaveSupplier(
            @Valid @ModelAttribute("supplier") PartyDto supplier,
            BindingResult bindingResult,
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long selectedSupplierId,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            addNavAttributes(model, selectedSupplierId);
            return "supplier";
        }

        try {
            partyProfileService.saveSupplierProfile(supplier.getId(), supplier);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            addNavAttributes(model, selectedSupplierId);
            return "supplier";
        }

        redirectAttributes.addAttribute("saved", "1");
        return "redirect:/suppliers";
    }

    // === CLIENTS LIST ===
    @GetMapping("/clients")
    public String listClients(
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            @RequestParam(value = "saved", required = false) String saved,
            @RequestParam(value = "deleted", required = false) String deleted,
            @RequestParam(name = "q", required = false) String query,
            Model model) {
        String searchQuery = query == null ? "" : query.trim();
        if (searchQuery.isEmpty()) {
            model.addAttribute("clients", partyProfileService.listClients());
        } else {
            model.addAttribute("clients", partyProfileService.searchClients(searchQuery));
        }
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("saved", saved != null);
        model.addAttribute("deleted", deleted != null);
        addNavAttributes(model, supplierId);

        return "clients";
    }

    // === NEW CLIENT ===
    @GetMapping("/clients/new")
    public String newClient(
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            Model model) {
        model.addAttribute("client", new PartyDto());
        model.addAttribute("isNew", true);
        addNavAttributes(model, supplierId);

        return "client-form";
    }

    // === EDIT CLIENT ===
    @GetMapping("/clients/{id}")
    public String editClient(
            @PathVariable Long id,
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            Model model) {
        PartyDto client = partyProfileService.findClientById(id).orElseGet(PartyDto::new);
        model.addAttribute("client", client);
        model.addAttribute("isNew", false);
        addNavAttributes(model, supplierId);

        return "client-form";
    }

    // === SAVE CLIENT ===
    @PostMapping("/clients")
    public String saveClient(
            @Valid @ModelAttribute("client") PartyDto client,
            BindingResult bindingResult,
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isNew", client.getId() == null);
            addNavAttributes(model, supplierId);
            return "client-form";
        }

        try {
            partyProfileService.saveClientProfile(client.getId(), client);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("isNew", client.getId() == null);
            addNavAttributes(model, supplierId);
            return "client-form";
        }

        redirectAttributes.addAttribute("saved", "1");
        return "redirect:/clients";
    }

    // === DELETE CLIENT (soft delete) ===
    @PostMapping("/clients/{id}/delete")
    public String deleteClient(
            @PathVariable Long id,
            @CookieValue(name = COOKIE_SUPPLIER_ID, required = false) Long supplierId,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            partyProfileService.deleteClient(id);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            addNavAttributes(model, supplierId);
            return "clients";
        }

        redirectAttributes.addAttribute("deleted", "1");
        return "redirect:/clients";
    }

    // === GET CLIENT INVOICE COUNT (for delete confirmation) ===
    @GetMapping("/clients/{id}/invoice-count")
    @ResponseBody
    public ResponseEntity<Long> getClientInvoiceCount(@PathVariable Long id) {
        return ResponseEntity.ok(partyProfileService.getClientInvoiceCount(id));
    }

    // === THEME TOGGLE ===
    @Hidden
    @PostMapping("/theme/toggle")
    @ResponseBody
    public ResponseEntity<Void> toggleTheme(
            @CookieValue(name = COOKIE_THEME, required = false) String currentTheme,
            HttpServletResponse response) {
        String newTheme = "dark".equals(currentTheme) ? "light" : "dark";
        Cookie cookie = new Cookie(COOKIE_THEME, newTheme);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    // === HELPER METHODS ===
    private void addNavAttributes(Model model, Long supplierId) {
        PartyDto currentSupplier = resolveSupplier(supplierId);
        model.addAttribute("currentSupplier", currentSupplier);
        model.addAttribute("currentSupplierId", supplierId);
    }

    private PartyDto resolveSupplier(Long supplierId) {
        if (supplierId != null) {
            return partyProfileService.findSupplierById(supplierId).orElse(null);
        }
        return partyProfileService.findSupplier().orElse(null);
    }

    private void addTemplateIndicatorAttributes(Model model, PartyDto supplier) {
        TemplateIndicator indicator = invoiceTemplateService.getTemplateIndicator(supplier);
        model.addAttribute("effectiveTemplateName", indicator.getTemplateName());
        model.addAttribute("effectiveTemplateSourceKey", indicator.getSourceKey());
    }
}
