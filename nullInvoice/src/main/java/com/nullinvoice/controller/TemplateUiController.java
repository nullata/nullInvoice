// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.dto.TemplateForm;
import com.nullinvoice.entity.InvoiceTemplates;
import com.nullinvoice.service.InvoiceTemplateService;
import com.nullinvoice.service.PartyProfileService;
import com.nullinvoice.service.TemplateService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Hidden
@Controller
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplateUiController {

    private final InvoiceTemplateService invoiceTemplateService;
    private final TemplateService templateService;
    private final PartyProfileService partyProfileService;

    @GetMapping
    public String listTemplates(
            @CookieValue(name = "selectedSupplierId", required = false) Long supplierId,
            Model model) {
        List<InvoiceTemplates> templates = invoiceTemplateService.listTemplates();
        TemplateForm form = new TemplateForm();
        model.addAttribute("templates", templates);
        model.addAttribute("templateForm", form);
        model.addAttribute("suppliers", partyProfileService.listSuppliers());
        model.addAttribute("globalDefaultTemplate", invoiceTemplateService.findDefaultTemplate());
        addNavAttributes(model, supplierId);
        return "templates";
    }

    @GetMapping("/{id}")
    public String editTemplate(
            @PathVariable Long id,
            @CookieValue(name = "selectedSupplierId", required = false) Long supplierId,
            Model model) {
        List<InvoiceTemplates> templates = invoiceTemplateService.listTemplates();
        InvoiceTemplates template = invoiceTemplateService.getTemplateById(id);

        TemplateForm form = new TemplateForm();
        form.setId(template.getId());
        form.setName(template.getName());
        form.setHtml(template.getHtml());
        form.setDefaultTemplate(template.getIsDefault());

        model.addAttribute("templates", templates);
        model.addAttribute("templateForm", form);
        model.addAttribute("suppliers", partyProfileService.listSuppliers());
        model.addAttribute("globalDefaultTemplate", invoiceTemplateService.findDefaultTemplate());
        addNavAttributes(model, supplierId);
        return "templates";
    }

    @PostMapping("/save")
    public String saveTemplate(@Valid @ModelAttribute("templateForm") TemplateForm form,
            BindingResult bindingResult,
            @CookieValue(name = "selectedSupplierId", required = false) Long supplierId,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("templates", invoiceTemplateService.listTemplates());
            model.addAttribute("suppliers", partyProfileService.listSuppliers());
            model.addAttribute("globalDefaultTemplate", invoiceTemplateService.findDefaultTemplate());
            addNavAttributes(model, supplierId);
            return "templates";
        }

        invoiceTemplateService.saveTemplate(form);
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/templates";
    }

    @PostMapping("/default")
    public String setSupplierDefaultTemplate(@RequestParam("supplierId") Long supplierId,
            @RequestParam(value = "templateId", required = false) String templateId,
            RedirectAttributes redirectAttributes) {
        try {
            partyProfileService.setSupplierDefaultTemplate(supplierId, templateId);
            redirectAttributes.addFlashAttribute("defaultSaved", true);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/templates";
    }

    @PostMapping(value = "/preview", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String previewTemplate(@RequestBody String html) {
        return templateService.renderPreview(html);
    }

    private void addNavAttributes(Model model, Long supplierId) {
        PartyDto currentSupplier = supplierId != null
                ? partyProfileService.findSupplierById(supplierId).orElse(null)
                : partyProfileService.findSupplier().orElse(null);
        model.addAttribute("currentSupplier", currentSupplier);
    }
}
