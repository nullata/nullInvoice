// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    const exportSection = document.getElementById('exportSection');
    const exportToggle = document.getElementById('exportToggle');
    const exportChevron = document.getElementById('exportChevron');
    const exportContent = document.getElementById('exportContent');
    const exportDisabledOverlay = document.getElementById('exportDisabledOverlay');
    const exportControls = document.getElementById('exportControls');
    const exportStartDate = document.getElementById('exportStartDate');
    const exportEndDate = document.getElementById('exportEndDate');
    const exportPreview = document.getElementById('exportPreview');
    const exportDownloadBtn = document.getElementById('exportDownloadBtn');
    const exportBtnIcon = document.getElementById('exportBtnIcon');
    const exportBtnText = document.getElementById('exportBtnText');
    const exportLimitWarning = document.getElementById('exportLimitWarning');
    const exportLimitWarningText = document.getElementById('exportLimitWarningText');
    const supplierFilter = document.getElementById('supplierFilter');

    if (!exportSection || !supplierFilter) {
        return;
    }

    const i18n = {
        found: exportSection.dataset.i18nFound || '{0} invoice(s) found',
        none: exportSection.dataset.i18nNone || 'No invoices in selected range',
        exceeds: exportSection.dataset.i18nExceeds || 'Exceeds limit of {0} invoices. Please narrow the date range.',
        selectSupplier: exportSection.dataset.i18nSelectSupplier || 'Select a supplier to enable export',
        downloading: exportSection.dataset.i18nDownloading || 'Downloading...',
        download: exportSection.dataset.i18nDownload || 'Download ZIP',
        error: exportSection.dataset.i18nError || 'Export failed'
    };

    let isExpanded = false;
    let debounceTimer = null;
    let currentPreview = null;
    let isDownloading = false;

    function toggleExpand() {
        isExpanded = !isExpanded;
        exportContent.classList.toggle('hidden', !isExpanded);
        if (isExpanded) {
            exportChevron.classList.remove('fa-chevron-down');
            exportChevron.classList.add('fa-chevron-up');
        } else {
            exportChevron.classList.remove('fa-chevron-up');
            exportChevron.classList.add('fa-chevron-down');
        }
    }

    function getSelectedSupplierId() {
        return supplierFilter.value || null;
    }

    function updateExportState() {
        const supplierId = getSelectedSupplierId();
        const hasSupplier = supplierId !== null && supplierId !== '';

        exportDisabledOverlay.classList.toggle('hidden', hasSupplier);
        exportControls.classList.toggle('hidden', !hasSupplier);

        if (hasSupplier) {
            fetchPreview();
        } else {
            currentPreview = null;
            exportPreview.textContent = '';
            exportDownloadBtn.disabled = true;
            exportLimitWarning.classList.add('hidden');
            exportStartDate.value = '';
            exportEndDate.value = '';
        }
    }

    function fetchPreview() {
        const supplierId = getSelectedSupplierId();
        if (!supplierId) {
            return;
        }

        const params = new URLSearchParams();
        params.set('supplierId', supplierId);
        if (exportStartDate.value) {
            params.set('startDate', exportStartDate.value);
        }
        if (exportEndDate.value) {
            params.set('endDate', exportEndDate.value);
        }

        fetch('/api/v1/invoices/export/preview?' + params.toString())
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Preview request failed');
                    }
                    return response.json();
                })
                .then(data => {
                    currentPreview = data;
                    updatePreviewUI(data);
                })
                .catch(error => {
                    //console.error('Failed to fetch export preview:', error);
                    currentPreview = null;
                    exportPreview.textContent = '';
                    exportDownloadBtn.disabled = true;
                });
    }

    function updatePreviewUI(data) {
        if (data.count === 0) {
            exportPreview.textContent = i18n.none;
            exportPreview.className = 'text-sm text-zinc-500';
            exportDownloadBtn.disabled = true;
            exportLimitWarning.classList.add('hidden');
        } else if (data.exceedsLimit) {
            exportPreview.textContent = i18n.found.replace('{0}', data.count);
            exportPreview.className = 'text-sm text-rose-400';
            exportDownloadBtn.disabled = true;
            exportLimitWarning.classList.remove('hidden');
            exportLimitWarningText.textContent = i18n.exceeds.replace('{0}', data.limit);
        } else {
            exportPreview.textContent = i18n.found.replace('{0}', data.count);
            exportPreview.className = 'text-sm text-emerald-400';
            exportDownloadBtn.disabled = false;
            exportLimitWarning.classList.add('hidden');
        }

        // set date placeholders from earliest/latest if fields are empty
        if (data.earliestDate && !exportStartDate.value) {
            exportStartDate.placeholder = data.earliestDate;
        }
        if (data.latestDate && !exportEndDate.value) {
            exportEndDate.placeholder = data.latestDate;
        }
    }

    function debouncedFetchPreview() {
        if (debounceTimer) {
            clearTimeout(debounceTimer);
        }
        debounceTimer = setTimeout(fetchPreview, 300);
    }

    function downloadZip() {
        if (isDownloading || exportDownloadBtn.disabled) {
            return;
        }

        const supplierId = getSelectedSupplierId();
        if (!supplierId) {
            return;
        }

        isDownloading = true;
        exportDownloadBtn.disabled = true;
        exportBtnIcon.className = 'fa-solid fa-spinner fa-spin';
        exportBtnText.textContent = i18n.downloading;

        const params = new URLSearchParams();
        params.set('supplierId', supplierId);
        if (exportStartDate.value) {
            params.set('startDate', exportStartDate.value);
        }
        if (exportEndDate.value) {
            params.set('endDate', exportEndDate.value);
        }

        fetch('/api/v1/invoices/export/zip?' + params.toString())
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Export request failed');
                    }
                    return response.blob().then(blob => {
                        const contentDisposition = response.headers.get('Content-Disposition');
                        let filename = 'invoices_export.zip';
                        if (contentDisposition) {
                            const filenameMatch = contentDisposition.match(/filename\*?=(?:UTF-8'')?["']?([^"';\n]+)/i);
                            if (filenameMatch && filenameMatch[1]) {
                                filename = decodeURIComponent(filenameMatch[1]);
                            }
                        }
                        return {blob, filename};
                    });
                })
                .then(({ blob, filename }) => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = filename;
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                    document.body.removeChild(a);
                })
                .catch(error => {
                    //console.error('Failed to download export:', error);
                    if (window.AppModal) {
                        window.AppModal.open({
                            tone: 'danger',
                            title: i18n.error,
                            message: error.message || i18n.error
                        });
                    }
                })
                .finally(() => {
                    isDownloading = false;
                    exportDownloadBtn.disabled = !currentPreview || currentPreview.count === 0 || currentPreview.exceedsLimit;
                    exportBtnIcon.className = 'fa-solid fa-download';
                    exportBtnText.textContent = i18n.download;
                });
    }

    // event listeners
    exportToggle.addEventListener('click', toggleExpand);
    supplierFilter.addEventListener('change', updateExportState);
    exportStartDate.addEventListener('change', debouncedFetchPreview);
    exportEndDate.addEventListener('change', debouncedFetchPreview);
    exportDownloadBtn.addEventListener('click', downloadZip);

    // init state
    updateExportState();
})();
