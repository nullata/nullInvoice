// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    const supplierData = document.getElementById('supplier-data');
    if (!supplierData) {
        return;
    }

    const copy = document.getElementById('invoiceFormCopy');
    const supplierMissing = supplierData.dataset.hasSupplier !== 'true';
    const defaultTaxRate = supplierData.dataset.defaultTaxRate;
    const generateBtn = document.getElementById('generateBtn');
    const dueDateInput = document.getElementById('dueDate');
    const markUnpaidToggle = document.getElementById('markUnpaid');
    const clientSearch = document.getElementById('clientSearch');
    const clientSearchResults = document.getElementById('clientSearchResults');

    const modalTitleSuccess = copy && copy.dataset.modalTitleSuccess ? copy.dataset.modalTitleSuccess : 'Success';
    const modalTitleError = copy && copy.dataset.modalTitleError ? copy.dataset.modalTitleError : 'Error';
    const closeLabel = copy && copy.dataset.closeLabel ? copy.dataset.closeLabel : 'Close';
    const generateLabel = copy && copy.dataset.generateLabel ? copy.dataset.generateLabel : 'Generate Invoice';
    const generatingLabel = copy && copy.dataset.generatingLabel ? copy.dataset.generatingLabel : 'Generating...';
    const errorSupplierMissing = copy && copy.dataset.errorSupplierMissing ? copy.dataset.errorSupplierMissing : 'Supplier profile is missing. Configure it before generating invoices.';
    const errorClientRequired = copy && copy.dataset.errorClientRequired ? copy.dataset.errorClientRequired : 'Please fill in required client fields (name, country, address, city).';
    const errorItemsRequired = copy && copy.dataset.errorItemsRequired ? copy.dataset.errorItemsRequired : 'Add at least one invoice item.';
    const errorItemFieldsRequired = copy && copy.dataset.errorItemFieldsRequired ? copy.dataset.errorItemFieldsRequired : 'Each line item needs a description, quantity, and unit price greater than 0.';
    const errorGenerateFailed = copy && copy.dataset.errorGenerateFailed ? copy.dataset.errorGenerateFailed : 'Failed to generate invoice.';
    const errorNumberMissing = copy && copy.dataset.errorNumberMissing ? copy.dataset.errorNumberMissing : 'Invoice generated but number is missing in response.';
    const clientNoResults = copy && copy.dataset.clientNoResults ? copy.dataset.clientNoResults : 'No clients found';
    const clientLoadedTemplate = copy && copy.dataset.clientLoadedTemplate ? copy.dataset.clientLoadedTemplate : 'Loaded: {0}';

    // track selected client ID - cleared if user edits any client field
    let selectedClientId = null;

    function showStatusModal(title, message, tone) {
        if (!window.AppModal) {
            return;
        }
        window.AppModal.open({
            title: title,
            message: message,
            tone: tone === 'error' ? 'danger' : 'success',
            secondaryLabel: closeLabel
        });
    }

    function showError(message) {
        showStatusModal(modalTitleError, message, 'error');
    }

    function showSuccess(message) {
        showStatusModal(modalTitleSuccess, message, 'success');
    }

    function maybeShowTemplatesMissingModal() {
        if (!window.AppModal) {
            return;
        }
        const missingConfig = document.getElementById('templatesMissingConfig') || document.getElementById('defaultTemplateMissingConfig');
        if (!missingConfig) {
            return;
        }

        window.AppModal.open({
            title: missingConfig.dataset.title || 'Templates Missing',
            message: missingConfig.dataset.message || '',
            tone: 'warning',
            primaryLabel: missingConfig.dataset.primaryLabel || 'Go to Templates',
            primaryHref: missingConfig.dataset.primaryHref || '/templates',
            secondaryLabel: missingConfig.dataset.secondaryLabel || 'Cancel',
            secondaryHref: missingConfig.dataset.secondaryHref || '/',
            dismissible: false
        });
    }

    function closeAllDiscountPopovers() {
        document.querySelectorAll('.discount-popover').forEach(popover => {
            popover.classList.add('hidden');
        });
        document.querySelectorAll('.discount-helper').forEach(button => {
            button.setAttribute('aria-expanded', 'false');
        });
    }

    function wireDiscountHelper(row) {
        const helperButton = row.querySelector('.discount-helper');
        const popover = row.querySelector('.discount-popover');
        const percentInput = row.querySelector('.discount-percent');
        const applyButton = row.querySelector('.discount-apply');
        const message = row.querySelector('.discount-message');
        const quantityInput = row.querySelector('.item-qty');
        const priceInput = row.querySelector('.item-price');
        const discountInput = row.querySelector('.item-discount');

        if (!helperButton || !popover || !applyButton || !message) {
            return;
        }

        const hintMessage = message.dataset.hint || '';
        const missingMessage = message.dataset.missing || 'Enter quantity and unit price first.';
        const invalidMessage = message.dataset.invalid || 'Enter a valid percent.';
        const resultTemplate = message.dataset.resultTemplate || 'Discount amount: {0}';

        const setMessage = (text) => {
            message.textContent = text;
        };

        helperButton.addEventListener('click', (event) => {
            event.preventDefault();
            const isHidden = popover.classList.contains('hidden');
            closeAllDiscountPopovers();
            if (isHidden) {
                popover.classList.remove('hidden');
                helperButton.setAttribute('aria-expanded', 'true');
                setMessage(hintMessage);
                if (percentInput) {
                    percentInput.focus();
                }
            }
        });

        applyButton.addEventListener('click', (event) => {
            event.preventDefault();
            const percent = Number(percentInput ? percentInput.value : '');
            if (!Number.isFinite(percent) || percent < 0) {
                setMessage(invalidMessage);
                return;
            }
            const quantity = Number(quantityInput ? quantityInput.value : '');
            const unitPrice = Number(priceInput ? priceInput.value : '');
            if (!Number.isFinite(quantity) || quantity <= 0 || !Number.isFinite(unitPrice) || unitPrice <= 0) {
                setMessage(missingMessage);
                return;
            }
            const discountValue = (quantity * unitPrice * percent) / 100;
            if (discountInput) {
                discountInput.value = discountValue.toFixed(2);
            }
            setMessage(resultTemplate.replace('{0}', discountValue.toFixed(2)));
        });
    }

    function addItemRow() {
        const template = document.getElementById('item-template');
        if (!template) {
            return;
        }
        const clone = template.content.cloneNode(true);
        const row = clone.querySelector('.item-row');
        const taxInput = row.querySelector('.item-tax');
        row.querySelector('.remove-item').addEventListener('click', () => row.remove());
        if (taxInput && defaultTaxRate) {
            taxInput.value = defaultTaxRate;
        }
        wireDiscountHelper(row);
        document.getElementById('itemsContainer').appendChild(clone);
    }

    const addItemBtn = document.getElementById('addItemBtn');
    if (addItemBtn) {
        addItemBtn.addEventListener('click', addItemRow);
    }

    // add a default item
    addItemRow();

    function syncUnpaidToggle() {
        if (!markUnpaidToggle || !dueDateInput) {
            return;
        }
        const hasDueDate = Boolean(dueDateInput.value);
        markUnpaidToggle.disabled = !hasDueDate;
        if (!hasDueDate) {
            markUnpaidToggle.checked = false;
        }
    }

    if (dueDateInput && markUnpaidToggle) {
        dueDateInput.addEventListener('change', syncUnpaidToggle);
        syncUnpaidToggle();
    }

    // client search with debounce
    let searchTimeout;
    if (clientSearch && clientSearchResults) {
        clientSearch.addEventListener('input', () => {
            clearTimeout(searchTimeout);
            const q = clientSearch.value.trim();
            if (q.length < 2) {
                clientSearchResults.classList.add('hidden');
                return;
            }
            searchTimeout = setTimeout(async () => {
                try {
                    const response = await fetch('/api/v1/parties/clients/search?q=' + encodeURIComponent(q));
                    if (!response.ok) {
                        clientSearchResults.classList.add('hidden');
                        return;
                    }
                    const clients = await response.json();
                    if (clients.length === 0) {
                        clientSearchResults.innerHTML = '<div class="px-4 py-3 text-zinc-500 text-sm">' + clientNoResults + '</div>';
                    } else {
                        clientSearchResults.innerHTML = clients.map(b => `
              <div class="client-option px-4 py-3 hover:bg-zinc-800 cursor-pointer border-b border-zinc-800 last:border-0" data-client='${JSON.stringify(b)}'>
                <div class="font-medium">${b.name}</div>
                <div class="text-xs text-zinc-500">${b.taxId || b.vatId || b.city}</div>
              </div>
            `).join('');
                        document.querySelectorAll('.client-option').forEach(opt => {
                            opt.addEventListener('click', () => {
                                const client = JSON.parse(opt.dataset.client);
                                fillClientForm(client);
                                clientSearchResults.classList.add('hidden');
                                clientSearch.value = '';
                                const clientSearchMsg = document.getElementById('clientSearchMsg');
                                if (clientSearchMsg) {
                                    clientSearchMsg.textContent = clientLoadedTemplate.replace('{0}', client.name);
                                }
                            });
                        });
                    }
                    clientSearchResults.classList.remove('hidden');
                } catch (e) {
                    clientSearchResults.classList.add('hidden');
                }
            }, 300);
        });
    }

    // hide results on click outside
    document.addEventListener('click', (e) => {
        if (clientSearch && clientSearchResults && !clientSearch.contains(e.target) && !clientSearchResults.contains(e.target)) {
            clientSearchResults.classList.add('hidden');
        }
    });

    document.addEventListener('click', (event) => {
        if (event.target.closest('.discount-helper') || event.target.closest('.discount-popover')) {
            return;
        }
        closeAllDiscountPopovers();
    });

    function fillClientForm(client) {
        selectedClientId = client.id || null;
        document.getElementById('clientName').value = client.name || '';
        document.getElementById('clientCountry').value = client.country || '';
        document.getElementById('clientTaxId').value = client.taxId || '';
        document.getElementById('clientVatId').value = client.vatId || '';
        document.getElementById('clientAddress1').value = client.addressLine1 || '';
        document.getElementById('clientAddress2').value = client.addressLine2 || '';
        document.getElementById('clientCity').value = client.city || '';
        document.getElementById('clientRegion').value = client.region || '';
        document.getElementById('clientPostal').value = client.postalCode || '';
        document.getElementById('clientEmail').value = client.email || '';
        document.getElementById('clientPhone').value = client.phone || '';
    }

    // clear selected client ID if user edits any client field
    const clientFields = ['clientName', 'clientCountry', 'clientTaxId', 'clientVatId', 'clientAddress1',
        'clientAddress2', 'clientCity', 'clientRegion', 'clientPostal', 'clientEmail', 'clientPhone'];
    clientFields.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.addEventListener('input', () => {
                selectedClientId = null;
                const msg = document.getElementById('clientSearchMsg');
                if (msg)
                    msg.textContent = '';
            });
        }
    });

    function buildClient() {
        // if a client was selected from search and not edited, just send the ID
        if (selectedClientId) {
            return {id: selectedClientId};
        }
        return {
            name: document.getElementById('clientName').value.trim(),
            taxId: document.getElementById('clientTaxId').value.trim(),
            vatId: document.getElementById('clientVatId').value.trim(),
            addressLine1: document.getElementById('clientAddress1').value.trim(),
            addressLine2: document.getElementById('clientAddress2').value.trim(),
            city: document.getElementById('clientCity').value.trim(),
            region: document.getElementById('clientRegion').value.trim(),
            postalCode: document.getElementById('clientPostal').value.trim(),
            country: document.getElementById('clientCountry').value.trim(),
            email: document.getElementById('clientEmail').value.trim(),
            phone: document.getElementById('clientPhone').value.trim()
        };
    }

    function buildItems() {
        const rows = document.querySelectorAll('.item-row');
        const items = [];

        for (const row of rows) {
            const description = row.querySelector('.item-desc').value.trim();
            const quantityRaw = row.querySelector('.item-qty').value.trim();
            const unitPriceRaw = row.querySelector('.item-price').value.trim();
            const discountRaw = row.querySelector('.item-discount').value.trim();
            const taxRateRaw = row.querySelector('.item-tax').value.trim();

            const hasAnyInput = description || quantityRaw || unitPriceRaw || discountRaw || taxRateRaw;
            if (!hasAnyInput) {
                continue;
            }

            const quantity = Number(quantityRaw);
            const unitPrice = Number(unitPriceRaw);
            if (!description || !Number.isFinite(quantity) || quantity <= 0 || !Number.isFinite(unitPrice) || unitPrice <= 0) {
                return null;
            }

            items.push({
                description,
                quantity,
                unitPrice,
                discount: Number(discountRaw),
                taxRate: Number(taxRateRaw)
            });
        }

        return items;
    }

    if (!generateBtn) {
        return;
    }

    generateBtn.addEventListener('click', async () => {
        if (supplierMissing) {
            showError(errorSupplierMissing);
            return;
        }

        const client = buildClient();
        // skip validation if client was selected by ID, otherwise check required fields
        if (!client.id && (!client.name || !client.country || !client.addressLine1 || !client.city)) {
            showError(errorClientRequired);
            return;
        }

        const items = buildItems();
        if (items === null) {
            showError(errorItemFieldsRequired);
            return;
        }
        if (!items.length) {
            showError(errorItemsRequired);
            return;
        }

        const payload = {
            responseType: 'number',
            issueDate: document.getElementById('issueDate').value || null,
            dueDate: document.getElementById('dueDate').value || null,
            currencyCode: document.getElementById('currencyCode').value.trim() || null,
            supplierId: Number(supplierData.dataset.supplierId),
            client: client,
            items: items,
            notes: document.getElementById('notes').value.trim() || null
        };
        const markUnpaid = markUnpaidToggle && markUnpaidToggle.checked;
        const generateUrl = markUnpaid ? '/invoices/generate?unpaid=true' : '/invoices/generate';

        try {
            generateBtn.disabled = true;
            generateBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin mr-2"></i> ' + generatingLabel;

            const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

            const headers = {'Content-Type': 'application/json'};
            if (csrfToken && csrfHeader) {
                headers[csrfHeader] = csrfToken;
            }

            const response = await fetch(generateUrl, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const error = await response.json();
                showError(error.error || errorGenerateFailed);
                return;
            }

            const data = await response.json();
            const invoiceNumber = data.invoice_number || data.invoiceNumber;
            if (!invoiceNumber) {
                showError(errorNumberMissing);
                return;
            }
            const supplierId = supplierData ? supplierData.dataset.supplierId : '';
            const supplierQuery = supplierId ? '?supplierId=' + encodeURIComponent(supplierId) : '';
            window.location.href = '/invoices/' + encodeURIComponent(invoiceNumber) + supplierQuery;
            return;
        } catch (err) {
            showError(errorGenerateFailed);
        } finally {
            generateBtn.disabled = false;
            generateBtn.innerHTML = '<i class="fa-solid fa-file-invoice mr-2"></i> ' + generateLabel;
        }
    });

    maybeShowTemplatesMissingModal();
})();
