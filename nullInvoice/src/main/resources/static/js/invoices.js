// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    const searchType = document.getElementById('searchType');
    const searchQuery = document.getElementById('searchQuery');
    if (!searchType || !searchQuery) {
        return;
    }

    const searchPlaceholder = searchQuery.dataset.placeholderText || 'Enter search value';
    const searchDatePlaceholder = searchQuery.dataset.placeholderDate || 'YYYY-MM-DD';

    function updateSearchInputType() {
        if (searchType.value === 'date') {
            searchQuery.type = 'date';
            searchQuery.placeholder = searchDatePlaceholder;
        } else {
            searchQuery.type = 'text';
            searchQuery.placeholder = searchPlaceholder;
        }
    }

    searchType.addEventListener('change', updateSearchInputType);
    updateSearchInputType();
})();
