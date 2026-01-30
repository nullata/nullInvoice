// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    let originalModalMessage = null;
    let i18nMessages = {};

    /**
     * Initialize admin page with i18n messages
     * @param {Object} i18n - Internationalized messages
     */
    window.AdminPage = {
        init: function (i18n) {
            i18nMessages = i18n;
        }
    };


    // reset the modal message element to its original state
    function resetModalMessage() {
        const messageEl = document.getElementById('appModalMessage');
        if (!originalModalMessage) {
            originalModalMessage = messageEl.cloneNode(true);
        }

        // if current element has been replaced - restore it
        if (messageEl.tagName !== 'P' || messageEl.id !== 'appModalMessage') {
            const parent = document.querySelector('#appModal .modal-surface .text-center');
            const currentMessage = parent.querySelector('#appModalMessage, [id="appModalMessage"]') ||
                    parent.querySelector('.mb-6');
            if (currentMessage) {
                currentMessage.replaceWith(originalModalMessage.cloneNode(true));
            }
        } else {
            messageEl.textContent = '';
            messageEl.className = 'text-zinc-300 mb-6 text-center';
        }
    }

    /**
     * Show the API key generation modal with the generated key
     * @param {string} apiKey - The generated API key
     */
    window.showApiKeyModal = function (apiKey) {
        if (!apiKey || apiKey.trim() === '') {
            // console.error('No API key provided to showApiKeyModal');
            return;
        }

        // console.log('Showing API key modal');
        resetModalMessage();

        window.AppModal.open({
            tone: 'warning',
            title: i18nMessages.modalGeneratedTitle,
            message: '',
            primaryLabel: i18nMessages.modalGeneratedConfirm,
            primaryAction: function () {
                window.AppModal.close();
                window.location.reload();
            },
            dismissible: false
        });

        // wait a tick for modal to fully open, then customize content
        setTimeout(() => {
            const messageEl = document.getElementById('appModalMessage');
            messageEl.className = 'mb-6';
            messageEl.innerHTML = `
                <p class="text-amber-400 font-semibold mb-3 text-center">
                    <i class="fa-solid fa-exclamation-triangle mr-2"></i>
                    ${i18nMessages.modalGeneratedWarning}
                </p>
                <div class="flex items-center gap-2 mb-2">
                    <input type="text" readonly id="apiKeyCode" value="${apiKey}"
                           class="input-field flex-1 px-3 py-2 bg-zinc-800 border border-zinc-700 rounded font-mono text-sm">
                    <button type="button" id="copyApiKeyBtn" onclick="copyApiKey()"
                            class="btn-surface px-3 py-2 bg-zinc-800 border border-zinc-700 rounded hover:bg-zinc-700 transition-colors">
                        <i class="fa-solid fa-copy"></i>
                    </button>
                </div>
                <p class="text-zinc-500 text-sm text-center">${i18nMessages.modalGeneratedUsage}</p>
            `;
        }, 50);
    };


    // copies the API key to clipboard
    window.copyApiKey = function () {
        const input = document.getElementById('apiKeyCode');
        const btn = document.getElementById('copyApiKeyBtn');

        navigator.clipboard.writeText(input.value).then(() => {
            // change to checkmark
            btn.innerHTML = '<i class="fa-solid fa-check text-emerald-400"></i>';
            btn.classList.add('bg-emerald-500/20');

            // revert after 2 seconds
            setTimeout(() => {
                btn.innerHTML = '<i class="fa-solid fa-copy"></i>';
                btn.classList.remove('bg-emerald-500/20');
            }, 2000);
        }).catch(err => {
            // console.error('Failed to copy:', err);
            alert('Failed to copy to clipboard');
        });
    };

    /**
     * Shows confirmation modal for revoking an API key
     * @param {HTMLElement} button - The revoke button that was clicked
     */
    window.confirmRevoke = function (button) {
        const keyId = button.dataset.keyId;

        resetModalMessage();

        window.AppModal.open({
            tone: 'danger',
            title: i18nMessages.modalRevokeTitle,
            message: i18nMessages.modalRevokeMessage,
            formAction: '/admin/api-keys/' + keyId + '/revoke',
            primaryLabel: i18nMessages.modalRevokeConfirm,
            secondaryLabel: i18nMessages.modalCancel
        });
    };
})();
