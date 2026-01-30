// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.dto.PartyDto;
import com.nullinvoice.dto.TemplateForm;
import com.nullinvoice.dto.TemplateIndicator;
import com.nullinvoice.entity.InvoiceTemplates;
import com.nullinvoice.entity.Parties;
import com.nullinvoice.error.ConfigurationException;
import com.nullinvoice.repository.InvoiceTemplateRepository;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceTemplateService {

    private final InvoiceTemplateRepository repository;

    @Transactional(readOnly = true)
    public List<InvoiceTemplates> listTemplates() {
        return repository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public InvoiceTemplates resolveTemplate(String name) {
        if (name == null || name.isBlank()) {
            return getDefaultTemplate();
        }
        return repository.findByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new IllegalArgumentException("template not found: " + name));
    }

    @Transactional(readOnly = true)
    public InvoiceTemplates resolveTemplate(String name, Parties supplier) {
        if (name == null || name.isBlank()) {
            if (supplier != null && supplier.getDefaultTemplateId() != null) {
                return supplier.getDefaultTemplateId();
            }
            return getDefaultTemplate();
        }
        return repository.findByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new IllegalArgumentException("template not found: " + name));
    }

    @Transactional(readOnly = true)
    public InvoiceTemplates getDefaultTemplate() {
        return repository.findFirstByIsDefaultTrue()
                .orElseThrow(() -> new ConfigurationException(
                "No default invoice template configured. Please create a template and set it as default."));
    }

    @Transactional(readOnly = true)
    public boolean hasDefaultTemplate() {
        return repository.findFirstByIsDefaultTrue().isPresent();
    }

    @Transactional(readOnly = true)
    public InvoiceTemplates findDefaultTemplate() {
        return repository.findFirstByIsDefaultTrue().orElse(null);
    }

    @Transactional(readOnly = true)
    public InvoiceTemplates getTemplateById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("template not found"));
    }

    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    @Transactional
    public InvoiceTemplates saveTemplate(TemplateForm form) {
        boolean isNew = form.getId() == null;
        InvoiceTemplates template = !isNew
                ? getTemplateById(form.getId())
                : new InvoiceTemplates();

        template.setName(form.getName().trim());
        template.setHtml(ensureXmlDeclaration(form.getHtml()));
        template.setIsDefault(form.isDefaultTemplate());
        if (isNew) {
            template.setCreatedAt(new Date());
        }
        template.setUpdatedAt(new Date());

        InvoiceTemplates saved = repository.save(template);
        if (saved.getIsDefault()) {
            repository.clearDefaultExcept(saved.getId());
        }

        return saved;
    }

    /**
     * Resolves the effective template for a supplier
     * Prefers supplier's default template, falls back to global default
     * @param supplier
     * @return 
     */
    @Transactional(readOnly = true)
    public InvoiceTemplates resolveEffectiveTemplate(PartyDto supplier) {
        if (supplier != null && supplier.getDefaultTemplateId() != null) {
            try {
                return getTemplateById(supplier.getDefaultTemplateId());
            } catch (IllegalArgumentException ignored) {
                // fall through to global default
            }
        }
        return findDefaultTemplate();
    }

    /**
     * Returns template indicator info showing which template is effective and its source
     * @param supplier
     * @return 
     */
    @Transactional(readOnly = true)
    public TemplateIndicator getTemplateIndicator(PartyDto supplier) {
        if (supplier == null || supplier.getId() == null) {
            return new TemplateIndicator(null, null);
        }

        InvoiceTemplates supplierTemplate = null;
        if (supplier.getDefaultTemplateId() != null) {
            try {
                supplierTemplate = getTemplateById(supplier.getDefaultTemplateId());
            } catch (IllegalArgumentException ignored) {
                // supplier template not found
            }
        }

        InvoiceTemplates globalTemplate = findDefaultTemplate();
        InvoiceTemplates effectiveTemplate = supplierTemplate != null ? supplierTemplate : globalTemplate;

        String templateName = effectiveTemplate != null ? effectiveTemplate.getName() : null;
        String sourceKey = supplierTemplate != null ? "supplier" : (globalTemplate != null ? "global" : null);

        return new TemplateIndicator(templateName, sourceKey);
    }

    /**
     * Ensures the template HTML starts with an XML declaration
     * Required for Flying Saucer PDF generation
     */
    private String ensureXmlDeclaration(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        String trimmed = html.trim();
        if (trimmed.startsWith("<?xml")) {
            return html;
        }
        return XML_DECLARATION + "\n" + html;
    }
}
