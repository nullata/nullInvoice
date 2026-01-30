// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    if (!window.AppModal) {
        return;
    }

    // try to get config from either suppliers list page or single supplier page
    const listConfig = document.getElementById('suppliersModalConfig');
    const detailConfig = document.getElementById('supplierModalConfig');
    const config = listConfig || detailConfig;

    if (!config) {
        return;
    }

    // get i18n messages from config
    const title = config.dataset.title || 'Delete Supplier?';
    const deleteDefaultText = config.dataset.defaultText || 'Are you sure you want to delete this supplier?';
    const deleteConfirmTemplate = config.dataset.confirmTemplate || 'Are you sure you want to delete "{name}"?';
    const deleteWithInvoicesTemplate = config.dataset.withInvoicesTemplate || '"{name}" has {count} invoice(s). Deleting will hide the supplier but preserve invoice data.';
    const primaryLabel = config.dataset.primaryLabel || 'Yes, Delete';
    const secondaryLabel = config.dataset.secondaryLabel || 'Cancel';

    /**
     * Opens delete confirmation modal for a supplier
     * @param {string} supplierId - The supplier ID
     * @param {string|null} supplierName - The supplier name (optional, for better messages)
     */
    function confirmDelete(supplierId, supplierName) {
        const formAction = '/suppliers/' + supplierId + '/delete';

        fetch('/suppliers/' + supplierId + '/invoice-count')
            .then(r => r.json())
            .then(count => {
                let message;
                if (count > 0 && supplierName) {
                    message = deleteWithInvoicesTemplate
                        .replace('{name}', supplierName)
                        .replace('{count}', count);
                } else if (count > 0) {
                    message = deleteWithInvoicesTemplate
                        .replace('"{name}" has', 'This supplier has')
                        .replace('{count}', count);
                } else if (supplierName) {
                    message = deleteConfirmTemplate.replace('{name}', supplierName);
                } else {
                    message = deleteDefaultText;
                }

                window.AppModal.open({
                    title: title,
                    message: message,
                    tone: 'danger',
                    formAction: formAction,
                    primaryLabel: primaryLabel,
                    secondaryLabel: secondaryLabel
                });
            })
            .catch(() => {
                const message = supplierName
                    ? deleteConfirmTemplate.replace('{name}', supplierName)
                    : deleteDefaultText;

                window.AppModal.open({
                    title: title,
                    message: message,
                    tone: 'danger',
                    formAction: formAction,
                    primaryLabel: primaryLabel,
                    secondaryLabel: secondaryLabel
                });
            });
    }

    // handle suppliers list page (multiple delete buttons)
    document.querySelectorAll('.delete-supplier-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const supplierId = this.getAttribute('data-supplier-id');
            const supplierName = this.getAttribute('data-supplier-name');
            confirmDelete(supplierId, supplierName);
        });
    });

    // handle single supplier detail page (one delete button)
    const deleteBtn = document.getElementById('deleteBtn');
    if (deleteBtn && detailConfig) {
        const supplierId = detailConfig.dataset.supplierId;
        if (supplierId) {
            deleteBtn.addEventListener('click', function () {
                confirmDelete(supplierId, null);
            });
        }
    }
})();
