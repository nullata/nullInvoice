// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function() {
    const previewFrame = document.getElementById('previewFrame');
    const previewStatus = document.getElementById('previewStatus');
    const htmlInput = document.getElementById('templateHtml');
    const previewConfig = document.getElementById('templatePreviewConfig');
    if (!previewFrame || !previewStatus || !htmlInput) {
        return;
    }

    const templatePreviewEmpty = previewConfig && previewConfig.dataset.previewEmpty
        ? previewConfig.dataset.previewEmpty
        : 'Preview unavailable';

    // critical placeholders that should be present in a template
    const criticalPlaceholders = [
        { key: 'invoiceNumber', label: 'Invoice Number' },
        { key: 'supplierName', label: 'Supplier Name' },
        { key: 'clientName', label: 'Client Name' },
        { key: 'total', label: 'Total' },
        { key: 'itemsRows', label: 'Items Rows' }
    ];

    // create or get warning container
    let warningContainer = document.getElementById('templateWarnings');
    if (!warningContainer) {
        warningContainer = document.createElement('div');
        warningContainer.id = 'templateWarnings';
        warningContainer.className = 'mb-4';
        htmlInput.parentElement.insertBefore(warningContainer, htmlInput);
    }

    function validateTemplate(html) {
        const warnings = [];
        const errors = [];

        if (!html || !html.trim()) {
            warningContainer.innerHTML = '';
            return { warnings, errors };
        }

        // check for XML declaration
        if (!html.trim().startsWith('<?xml')) {
            warnings.push('Missing XML declaration (<?xml version="1.0" encoding="UTF-8"?>). It will be added automatically when saving.');
        }

        // check for XHTML namespace
        if (!html.includes('xmlns="http://www.w3.org/1999/xhtml"') && !html.includes("xmlns='http://www.w3.org/1999/xhtml'")) {
            errors.push('Missing XHTML namespace. Add xmlns="http://www.w3.org/1999/xhtml" to your &lt;html&gt; tag for PDF generation to work.');
        }

        // check for critical placeholders
        const missingPlaceholders = [];
        for (const placeholder of criticalPlaceholders) {
            if (!html.includes('{{' + placeholder.key + '}}')) {
                missingPlaceholders.push(placeholder.label);
            }
        }
        if (missingPlaceholders.length > 0) {
            warnings.push('Missing placeholders: ' + missingPlaceholders.join(', '));
        }

        // render warnings/errors
        let html_out = '';
        for (const error of errors) {
            html_out += '<div class="p-3 rounded-lg bg-rose-500/10 border border-rose-500/30 text-rose-400 text-sm mb-2">' +
                '<i class="fa-solid fa-circle-exclamation mr-2"></i>' + error + '</div>';
        }
        for (const warning of warnings) {
            html_out += '<div class="p-3 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-400 text-sm mb-2">' +
                '<i class="fa-solid fa-triangle-exclamation mr-2"></i>' + warning + '</div>';
        }
        warningContainer.innerHTML = html_out;

        return { warnings, errors };
    }

    // sample data for preview
    const sampleData = {
        'invoice.invoiceNumber': 'INV-000001',
        'invoice.issueDate': '01/15/2025',
        'invoice.dueDate': '01/29/2025',
        'invoice.subtotal': '$100.00',
        'invoice.taxTotal': '$20.00',
        'invoice.total': '$120.00',
        'invoice.notes': 'Thank you for your business.',
        'invoice.supplier.name': 'Acme Corporation',
        'invoice.supplier.taxId': '123456789',
        'invoice.supplier.vatId': 'BG123456789',
        'invoice.supplier.addressLine1': '123 Main Street',
        'invoice.supplier.addressLine2': 'Suite 100',
        'invoice.supplier.city': 'New York',
        'invoice.supplier.region': 'NY',
        'invoice.supplier.postalCode': '10001',
        'invoice.supplier.country': 'USA',
        'invoice.supplier.email': 'billing@acme.com',
        'invoice.supplier.phone': '+1 555-123-4567',
        'invoice.client.name': 'Client Company Ltd',
        'invoice.client.taxId': '987654321',
        'invoice.client.vatId': 'GB987654321',
        'invoice.client.addressLine1': '456 Oak Avenue',
        'invoice.client.addressLine2': '',
        'invoice.client.city': 'London',
        'invoice.client.region': '',
        'invoice.client.postalCode': 'SW1A 1AA',
        'invoice.client.country': 'UK',
        'invoice.client.email': 'accounts@client.com',
        'invoice.client.phone': '+44 20 7123 4567'
    };

    // sample line items
    const sampleItems = [
        { lineNumber: 1, description: 'Consulting Services', quantity: '10', unitPrice: '$50.00', taxRate: '20%', lineTotal: '$600.00' },
        { lineNumber: 2, description: 'Software License', quantity: '1', unitPrice: '$200.00', taxRate: '20%', lineTotal: '$240.00' }
    ];

    function renderPreview() {
        let html = htmlInput.value;
        if (!html.trim()) {
            previewFrame.srcdoc = '<p style="padding:16px;font-family:system-ui;color:#64748b">' + templatePreviewEmpty + '</p>';
            previewStatus.textContent = templatePreviewEmpty;
            return;
        }

        previewStatus.textContent = 'Rendering...';

        // replace simple ${...} expressions
        for (const [key, value] of Object.entries(sampleData)) {
            const regex = new RegExp('\\$\\{' + key.replace('.', '\\.') + '\\}', 'g');
            html = html.replace(regex, value || '');
        }

        // handle th:text attributes by replacing element content
        html = html.replace(/th:text="\$\{([^}]+)\}"/g, (match, expr) => {
            const value = sampleData[expr] || '';
            return `data-preview-text="${value}"`;
        });

        // handle th:if for hasDueDate
        html = html.replace(/th:if="\$\{invoice\.hasDueDate\(\)\}"/g, '');
        html = html.replace(/th:if="\$\{invoice\.hasDueDate\}"/g, '');

        // handle th:each for items - generate sample rows
        const itemRowMatch = html.match(/<tr[^>]*th:each="item\s*:\s*\$\{invoice\.items\}"[^>]*>([\s\S]*?)<\/tr>/i);
        if (itemRowMatch) {
            let itemRowTemplate = itemRowMatch[0];
            let generatedRows = '';

            for (const item of sampleItems) {
                let row = itemRowTemplate;
                row = row.replace(/th:each="[^"]*"/g, '');
                row = row.replace(/\$\{item\.lineNumber\}/g, item.lineNumber);
                row = row.replace(/\$\{item\.description\}/g, item.description);
                row = row.replace(/\$\{item\.quantity\}/g, item.quantity);
                row = row.replace(/\$\{item\.unitPrice\}/g, item.unitPrice);
                row = row.replace(/\$\{item\.taxRate\}/g, item.taxRate);
                row = row.replace(/\$\{item\.lineTotal\}/g, item.lineTotal);
                row = row.replace(/th:text="\$\{[^}]+\}"/g, '');
                generatedRows += row;
            }

            html = html.replace(itemRowMatch[0], generatedRows);
        }

        // clean up remaining th: attributes
        html = html.replace(/\s*th:[a-z]+="[^"]*"/g, '');

        // apply data-preview-text values
        const wrapper = document.createElement('div');
        wrapper.innerHTML = html;
        wrapper.querySelectorAll('[data-preview-text]').forEach(el => {
            el.textContent = el.getAttribute('data-preview-text');
            el.removeAttribute('data-preview-text');
        });
        html = wrapper.innerHTML;

        previewFrame.srcdoc = html;
        previewStatus.textContent = 'Updated';
    }

    let debounceHandle;
    htmlInput.addEventListener('input', () => {
        clearTimeout(debounceHandle);
        debounceHandle = setTimeout(() => {
            validateTemplate(htmlInput.value);
            renderPreview();
        }, 300);
    });

    window.addEventListener('load', () => {
        validateTemplate(htmlInput.value);
        renderPreview();
    });
})();
