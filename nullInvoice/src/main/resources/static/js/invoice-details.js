// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    const snapshotFrame = document.getElementById('snapshotFrame');
    const snapshotStatus = document.getElementById('snapshotStatus');
    const snapshotTemplate = document.getElementById('invoiceHtmlSnapshot');
    const snapshotConfig = document.getElementById('snapshotConfig');
    if (!snapshotFrame || !snapshotStatus) {
        return;
    }

    const snapshotStatusLoaded = snapshotConfig && snapshotConfig.dataset.statusLoaded
            ? snapshotConfig.dataset.statusLoaded
            : 'Loaded';
    const snapshotStatusEmpty = snapshotConfig && snapshotConfig.dataset.statusEmpty
            ? snapshotConfig.dataset.statusEmpty
            : 'Empty';
    const snapshotEmptyHtml = snapshotConfig && snapshotConfig.dataset.emptyHtml
            ? snapshotConfig.dataset.emptyHtml
            : 'No HTML snapshot stored for this invoice.';

    function renderSnapshot() {
        if (!snapshotFrame) {
            return;
        }

        const html = snapshotTemplate ? snapshotTemplate.innerHTML.trim() : '';
        if (!html) {
            snapshotFrame.srcdoc = '<p style="padding:16px;font-family:system-ui;color:#64748b">' + snapshotEmptyHtml + '</p>';
            snapshotStatus.textContent = snapshotStatusEmpty;
            return;
        }

        snapshotFrame.srcdoc = html;
        snapshotStatus.textContent = snapshotStatusLoaded;
    }

    window.addEventListener('load', renderSnapshot);
})();
