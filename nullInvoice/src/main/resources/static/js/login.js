// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    const hintButton = document.getElementById('hintButton');
    const hintTooltip = document.getElementById('hintTooltip');
    const hintContent = document.getElementById('hintContent');
    const usernameInput = document.getElementById('username');

    if (!hintButton || !hintTooltip || !hintContent || !usernameInput) {
        return;
    }

    let hintMessages = {};

    /**
     * Initialize login page with i18n messages
     * @param {Object} messages - Internationalized messages
     */
    window.LoginPage = {
        init: function (messages) {
            hintMessages = messages;
        }
    };

    // enable hint button when username is entered
    usernameInput.addEventListener('input', () => {
        hintButton.disabled = !usernameInput.value.trim();
    });


    // show/hide hint on click
    hintButton.addEventListener('click', async () => {
        if (hintTooltip.classList.contains('hidden')) {
            const username = usernameInput.value.trim();
            if (!username) {
                //console.log('No username entered');
                return;
            }

            // show tooltip with loading message
            hintContent.textContent = hintMessages.loading;
            hintTooltip.classList.remove('hidden');

            // fetch hint
            try {
                // console.log('Fetching hint for username:', username);
                const response = await fetch('/login/hint?username=' + encodeURIComponent(username));
                // console.log('Response status:', response.status);

                if (!response.ok) {
                    throw new Error('Failed to fetch hint');
                }

                const hint = await response.text();
                // console.log('Received hint:', hint);
                hintContent.textContent = hint;
            } catch (error) {
                // console.error('Error fetching hint:', error);
                hintContent.textContent = hintMessages.none;
            }
        } else {
            hintTooltip.classList.add('hidden');
        }
    });

    // hide tooltip when clicking outside
    document.addEventListener('click', (e) => {
        if (!hintButton.contains(e.target) && !hintTooltip.contains(e.target)) {
            hintTooltip.classList.add('hidden');
        }
    });
})();
