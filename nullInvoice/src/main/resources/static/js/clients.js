// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    const config = document.getElementById('clientsModalConfig');
    if (!config || !window.AppModal) {
        return;
    }

    const title = config.dataset.title || 'Delete Client?';
    const deleteDefaultText = config.dataset.defaultText || 'Are you sure you want to delete this client?';
    const deleteConfirmTemplate = config.dataset.confirmTemplate || 'Are you sure you want to delete "{name}"?';
    const deleteWithInvoicesTemplate = config.dataset.withInvoicesTemplate || '"{name}" has {count} invoice(s). Deleting will hide the client but preserve invoice data.';
    const primaryLabel = config.dataset.primaryLabel || 'Yes, Delete';
    const secondaryLabel = config.dataset.secondaryLabel || 'Cancel';

    document.querySelectorAll('.delete-client-btn').forEach(btn => {
        btn.addEventListener('click', function () {
            const clientId = this.getAttribute('data-client-id');
            const clientName = this.getAttribute('data-client-name');
            const formAction = '/clients/' + clientId + '/delete';

            fetch('/clients/' + clientId + '/invoice-count')
                    .then(r => r.json())
                    .then(count => {
                        const message = count > 0
                                ? deleteWithInvoicesTemplate.replace('{name}', clientName).replace('{count}', count)
                                : deleteConfirmTemplate.replace('{name}', clientName);
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
                        const message = clientName
                                ? deleteConfirmTemplate.replace('{name}', clientName)
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
        });
    });
})();
