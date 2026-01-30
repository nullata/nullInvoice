// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    const modal = document.getElementById('appModal');
    if (!modal) {
        return;
    }

    const icon = document.getElementById('appModalIcon');
    const title = document.getElementById('appModalTitle');
    const message = document.getElementById('appModalMessage');
    const form = document.getElementById('appModalForm');
    const primarySubmit = document.getElementById('appModalPrimarySubmit');
    const primaryLink = document.getElementById('appModalPrimaryLink');
    const primaryButton = document.getElementById('appModalPrimaryButton');
    const secondaryLink = document.getElementById('appModalSecondaryLink');
    const secondaryButton = document.getElementById('appModalSecondaryButton');

    const toneStyles = {
        info: {
            icon: 'fa-circle-info text-sky-400',
            title: 'text-sky-400',
            primary: 'bg-sky-500 text-white hover:bg-sky-400'
        },
        success: {
            icon: 'fa-circle-check text-emerald-400',
            title: 'text-emerald-400',
            primary: 'bg-emerald-500 text-zinc-900 hover:bg-emerald-400'
        },
        warning: {
            icon: 'fa-triangle-exclamation text-amber-400',
            title: 'text-amber-400',
            primary: 'bg-amber-500 text-zinc-900 hover:bg-amber-400'
        },
        danger: {
            icon: 'fa-triangle-exclamation text-rose-400',
            title: 'text-rose-400',
            primary: 'bg-rose-600 text-white hover:bg-rose-500'
        }
    };

    function resetActions() {
        form.classList.add('hidden');
        primaryLink.classList.add('hidden');
        primaryButton.classList.add('hidden');
        secondaryLink.classList.add('hidden');
        secondaryButton.classList.remove('hidden');
        primarySubmit.textContent = '';
        primaryLink.textContent = '';
        primaryButton.textContent = '';
        secondaryLink.textContent = '';
        secondaryButton.textContent = '';
        primaryButton.onclick = null;
        secondaryButton.onclick = null;
    }

    function applyTone(tone) {
        const styles = toneStyles[tone] || toneStyles.info;
        icon.className = 'fa-solid ' + styles.icon + ' text-4xl mb-3';
        title.className = 'text-xl font-bold ' + styles.title;
        const primaryClass = 'px-6 py-2.5 rounded-lg font-semibold transition-colors ' + styles.primary;
        primarySubmit.className = primaryClass;
        primaryLink.className = primaryClass;
        primaryButton.className = primaryClass;
    }

    function closeModal() {
        modal.classList.add('hidden');
    }

    function openModal(config) {
        const opts = config || {};
        const tone = opts.tone || 'info';

        applyTone(tone);
        title.textContent = opts.title || '';
        message.textContent = opts.message || '';
        resetActions();

        if (opts.formAction) {
            form.action = opts.formAction;
            form.classList.remove('hidden');
            primarySubmit.textContent = opts.primaryLabel || 'Confirm';
        } else if (opts.primaryHref) {
            primaryLink.href = opts.primaryHref;
            primaryLink.textContent = opts.primaryLabel || 'OK';
            primaryLink.classList.remove('hidden');
        } else if (opts.primaryAction) {
            primaryButton.textContent = opts.primaryLabel || 'OK';
            primaryButton.classList.remove('hidden');
            primaryButton.onclick = opts.primaryAction;
        } else {
            primaryButton.classList.add('hidden');
        }

        if (opts.secondaryHref) {
            secondaryLink.href = opts.secondaryHref;
            secondaryLink.textContent = opts.secondaryLabel || 'Cancel';
            secondaryLink.classList.remove('hidden');
            secondaryButton.classList.add('hidden');
        } else {
            secondaryButton.textContent = opts.secondaryLabel || 'Close';
            secondaryButton.onclick = opts.secondaryAction || closeModal;
        }

        modal.classList.remove('hidden');
        modal.dataset.dismissible = opts.dismissible === false ? 'false' : 'true';
    }

    modal.addEventListener('click', (event) => {
        if (event.target !== modal) {
            return;
        }
        if (modal.dataset.dismissible === 'false') {
            return;
        }
        closeModal();
    });

    document.addEventListener('keydown', (event) => {
        if (event.key !== 'Escape') {
            return;
        }
        if (modal.classList.contains('hidden')) {
            return;
        }
        if (modal.dataset.dismissible === 'false') {
            return;
        }
        closeModal();
    });

    window.AppModal = {
        open: openModal,
        close: closeModal
    };
})();
