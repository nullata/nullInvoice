// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
(function () {
    // ensure the date picker icon stays visible in dark mode (WebKit)
    if (!document.getElementById("date-picker-dark-style")) {
        const style = document.createElement("style");
        style.id = "date-picker-dark-style";
        style.textContent =
                '.dark input[type="date"]::-webkit-calendar-picker-indicator{filter:invert(1);opacity:0.9;}';
        document.head.appendChild(style);
    }

    const themeToggle = document.getElementById("themeToggle");
    const darkIcon = document.getElementById("darkIcon");
    const lightIcon = document.getElementById("lightIcon");
    const html = document.documentElement;
    const body = document.body;

    // theme class mappings - defines what classes to add/remove for each theme
    const themeConfig = {
        light: {
            html: {add: ["light"], remove: ["dark"]},
            body: {
                add: ["bg-gray-50", "text-gray-900"],
                remove: [
                    "bg-zinc-950",
                    "bg-slate-950",
                    "text-zinc-100",
                    "text-slate-100",
                ],
            },
            ".nav-bar": {
                add: ["bg-white/90", "border-gray-300"],
                remove: [
                    "bg-zinc-900/80",
                    "bg-slate-900/80",
                    "border-zinc-700",
                    "border-slate-700",
                ],
            },
            ".nav-link": {
                add: ["text-gray-600"],
                remove: ["text-zinc-300", "text-slate-300"],
            },
            ".mobile-nav": {
                add: ["bg-white/95", "border-gray-300"],
                remove: [
                    "bg-zinc-900/95",
                    "bg-slate-900/95",
                    "border-zinc-700",
                    "border-slate-700",
                ],
            },
            ".card": {
                add: ["bg-white", "border-gray-300"],
                remove: [
                    "bg-zinc-900",
                    "bg-slate-900",
                    "border-zinc-800",
                    "border-slate-800",
                ],
            },
            ".modal-surface": {
                add: ["bg-white", "border-gray-300"],
                remove: [
                    "bg-zinc-900",
                    "bg-slate-900",
                    "border-zinc-700",
                    "border-slate-700",
                ],
            },
            ".modal-overlay": {add: ["bg-black/40"], remove: ["bg-black/60"]},
            ".hint-tooltip": {
                add: ["bg-white", "border-gray-300"],
                remove: [
                    "bg-zinc-800",
                    "bg-slate-800",
                    "border-zinc-700",
                    "border-slate-700",
                ],
            },
            '.input-field, input[type="text"], input[type="password"], input[type="email"], input[type="date"], input[type="number"], input[type="tel"], textarea, select':
                    {
                        add: ["bg-white", "border-gray-400", "text-gray-900"],
                        remove: [
                            "bg-zinc-950",
                            "bg-zinc-800",
                            "bg-slate-950",
                            "border-zinc-700",
                            "border-slate-700",
                            "text-zinc-100",
                            "text-slate-100",
                        ],
                    },
            ".config-item, .info-box": {
                add: ["bg-gray-100", "border-gray-300"],
                remove: [
                    "bg-zinc-950",
                    "bg-zinc-800",
                    "bg-slate-950",
                    "border-zinc-700",
                    "border-slate-700",
                ],
            },
            ".item-row": {
                add: ["bg-gray-100", "border-gray-300"],
                remove: [
                    "bg-zinc-950",
                    "bg-slate-950",
                    "border-zinc-800",
                    "border-slate-800",
                ],
            },
            table: {
                add: ["divide-gray-300"],
                remove: ["divide-zinc-800", "divide-slate-800"],
            },
            thead: {
                add: ["bg-gray-100"],
                remove: ["bg-zinc-800/50", "bg-slate-800/50"],
            },
            tbody: {
                add: ["divide-gray-200"],
                remove: ["divide-zinc-800", "divide-slate-800"],
            },
            th: {
                add: ["text-gray-600"],
                remove: ["text-zinc-400", "text-slate-400"],
            },
            ".text-zinc-400, .text-slate-400": {
                add: ["text-gray-500"],
                remove: ["text-zinc-400", "text-slate-400"],
            },
            ".text-zinc-500, .text-slate-500": {
                add: ["text-gray-500"],
                remove: ["text-zinc-500", "text-slate-500"],
            },
            ".text-zinc-300, .text-slate-300": {
                add: ["text-gray-700"],
                remove: ["text-zinc-300", "text-slate-300"],
            },
            ".text-strong": {
                add: ["text-gray-900"],
                remove: ["text-zinc-100", "text-slate-100"],
            },
            ".text-total": {
                add: ["text-emerald-700"],
                remove: ["text-emerald-400"],
            },
            ".text-total-currency": {
                add: ["text-emerald-600"],
                remove: ["text-emerald-300"],
            },
            ".border-zinc-700, .border-slate-700": {
                add: ["border-gray-400"],
                remove: ["border-zinc-700", "border-slate-700"],
            },
            ".border-zinc-800, .border-slate-800": {
                add: ["border-gray-300"],
                remove: ["border-zinc-800", "border-slate-800"],
            },
            ".hover\\:bg-zinc-800, .hover\\:bg-slate-800": {
                add: ["hover:bg-gray-100"],
                remove: ["hover:bg-zinc-800", "hover:bg-slate-800"],
            },
            ".template-selected": {
                add: ["bg-gray-100"],
                remove: ["bg-zinc-800", "bg-slate-800"],
            },
            ".btn-surface": {
                add: [
                    "bg-gray-100",
                    "border-gray-300",
                    "text-gray-700",
                    "hover:bg-gray-200",
                ],
                remove: [
                    "bg-zinc-800",
                    "bg-slate-800",
                    "border-zinc-700",
                    "border-slate-700",
                    "text-zinc-100",
                    "text-slate-100",
                    "hover:bg-zinc-700",
                    "hover:bg-slate-700",
                ],
            },
            ".lang-selector-menu": {
                add: ["bg-white", "border-gray-300"],
                remove: [
                    "bg-zinc-900",
                    "bg-slate-900",
                    "border-zinc-700",
                    "border-slate-700",
                ],
            },
            ".lang-selector-option": {
                add: ["text-gray-700"],
                remove: ["text-zinc-300"],
            },
            ".btn-primary, #appModalPrimarySubmit, #appModalPrimaryButton, #appModalPrimaryLink":
                    {add: ["text-white"], remove: ["text-zinc-900"]},
            ".status-issued": {
                add: ["bg-emerald-100", "border-emerald-200", "text-emerald-700"],
                remove: [
                    "bg-emerald-500/20",
                    "border-emerald-500/30",
                    "text-emerald-400",
                ],
            },
            ".status-unpaid": {
                add: ["bg-amber-100", "border-amber-200", "text-amber-700"],
                remove: ["bg-amber-500/20", "border-amber-500/30", "text-amber-400"],
            },
            ".status-default": {
                add: ["bg-gray-100", "border-gray-200", "text-gray-600"],
                remove: ["bg-zinc-500/20", "border-zinc-500/30", "text-zinc-400"],
            },
            "#clientSearchResults": {
                add: ["bg-white", "border-gray-300"],
                remove: [
                    "bg-zinc-900",
                    "bg-slate-900",
                    "border-zinc-700",
                    "border-slate-700",
                ],
            },
            ".supplier-badge": {
                add: ["border-gray-400"],
                remove: ["border-zinc-700", "border-slate-700"],
            },
        },
        dark: {
            html: {add: ["dark"], remove: ["light"]},
            body: {
                add: ["bg-zinc-950", "text-zinc-100"],
                remove: [
                    "bg-gray-50",
                    "text-gray-900",
                    "bg-slate-950",
                    "text-slate-100",
                ],
            },
            ".nav-bar": {
                add: ["bg-zinc-900/80", "border-zinc-700"],
                remove: [
                    "bg-white/90",
                    "border-gray-300",
                    "bg-slate-900/80",
                    "border-slate-700",
                ],
            },
            ".nav-link": {
                add: ["text-zinc-300"],
                remove: ["text-gray-600", "text-slate-300"],
            },
            ".mobile-nav": {
                add: ["bg-zinc-900/95", "border-zinc-700"],
                remove: [
                    "bg-white/95",
                    "border-gray-300",
                    "bg-slate-900/95",
                    "border-slate-700",
                ],
            },
            ".card": {
                add: ["bg-zinc-900", "border-zinc-800"],
                remove: [
                    "bg-white",
                    "border-gray-300",
                    "bg-slate-900",
                    "border-slate-800",
                ],
            },
            ".modal-surface": {
                add: ["bg-zinc-900", "border-zinc-700"],
                remove: [
                    "bg-white",
                    "border-gray-300",
                    "bg-slate-900",
                    "border-slate-700",
                ],
            },
            ".modal-overlay": {add: ["bg-black/60"], remove: ["bg-black/40"]},
            ".hint-tooltip": {
                add: ["bg-zinc-800", "border-zinc-700"],
                remove: [
                    "bg-white",
                    "border-gray-300",
                    "bg-slate-800",
                    "border-slate-700",
                ],
            },
            '.input-field, input[type="text"], input[type="password"], input[type="email"], input[type="date"], input[type="number"], input[type="tel"], textarea, select':
                    {
                        add: ["bg-zinc-950", "border-zinc-700", "text-zinc-100"],
                        remove: [
                            "bg-white",
                            "bg-zinc-800",
                            "border-gray-400",
                            "text-gray-900",
                            "bg-slate-950",
                            "border-slate-700",
                            "text-slate-100",
                        ],
                    },
            ".config-item, .info-box": {
                add: ["bg-zinc-950", "border-zinc-700"],
                remove: [
                    "bg-gray-100",
                    "bg-zinc-800",
                    "bg-slate-950",
                    "border-gray-300",
                    "border-slate-700",
                ],
            },
            ".item-row": {
                add: ["bg-zinc-950", "border-zinc-800"],
                remove: [
                    "bg-gray-100",
                    "border-gray-300",
                    "bg-slate-950",
                    "border-slate-800",
                ],
            },
            table: {
                add: ["divide-zinc-800"],
                remove: ["divide-gray-300", "divide-slate-800"],
            },
            thead: {
                add: ["bg-zinc-800/50"],
                remove: ["bg-gray-100", "bg-slate-800/50"],
            },
            tbody: {
                add: ["divide-zinc-800"],
                remove: ["divide-gray-200", "divide-slate-800"],
            },
            th: {
                add: ["text-zinc-400"],
                remove: ["text-gray-600", "text-slate-400"],
            },
            ".text-gray-500": {add: ["text-zinc-400"], remove: ["text-gray-500"]},
            ".text-gray-700": {add: ["text-zinc-300"], remove: ["text-gray-700"]},
            ".text-strong": {add: ["text-zinc-100"], remove: ["text-gray-900"]},
            ".text-total": {
                add: ["text-emerald-400"],
                remove: ["text-emerald-700"],
            },
            ".text-total-currency": {
                add: ["text-emerald-300"],
                remove: ["text-emerald-600"],
            },
            ".border-gray-400": {
                add: ["border-zinc-700"],
                remove: ["border-gray-400"],
            },
            ".border-gray-300": {
                add: ["border-zinc-800"],
                remove: ["border-gray-300"],
            },
            ".hover\\:bg-gray-100": {
                add: ["hover:bg-zinc-800"],
                remove: ["hover:bg-gray-100"],
            },
            ".template-selected": {
                add: ["bg-zinc-800"],
                remove: ["bg-gray-100", "bg-slate-800"],
            },
            ".btn-surface": {
                add: [
                    "bg-zinc-800",
                    "border-zinc-700",
                    "text-zinc-100",
                    "hover:bg-zinc-700",
                ],
                remove: [
                    "bg-gray-100",
                    "border-gray-300",
                    "text-gray-700",
                    "hover:bg-gray-200",
                ],
            },
            ".lang-selector-menu": {
                add: ["bg-zinc-900", "border-zinc-700"],
                remove: [
                    "bg-white",
                    "border-gray-300",
                    "bg-slate-900",
                    "border-slate-700",
                ],
            },
            ".lang-selector-option": {
                add: ["text-zinc-300"],
                remove: ["text-gray-700", "text-emerald-700"],
            },
            ".btn-primary": {add: ["text-zinc-900"], remove: ["text-white"]},
            ".status-issued": {
                add: ["bg-emerald-500/20", "border-emerald-500/30", "text-emerald-400"],
                remove: ["bg-emerald-100", "border-emerald-200", "text-emerald-700"],
            },
            ".status-unpaid": {
                add: ["bg-amber-500/20", "border-amber-500/30", "text-amber-400"],
                remove: ["bg-amber-100", "border-amber-200", "text-amber-700"],
            },
            ".status-default": {
                add: ["bg-zinc-500/20", "border-zinc-500/30", "text-zinc-400"],
                remove: ["bg-gray-100", "border-gray-200", "text-gray-600"],
            },
            "#clientSearchResults": {
                add: ["bg-zinc-900", "border-zinc-700"],
                remove: [
                    "bg-white",
                    "border-gray-300",
                    "bg-slate-900",
                    "border-slate-700",
                ],
            },
            ".supplier-badge": {
                add: ["border-zinc-700"],
                remove: ["border-gray-400", "border-slate-700"],
            },
        },
    };

    // Special case handlers for elements that need custom logic
    const specialHandlers = {
        light: {
            ".lang-selector-option": (el) => {
                if (el.classList.contains("text-emerald-300")) {
                    el.classList.remove("text-emerald-300");
                    el.classList.add("text-emerald-700");
                }
            },
        },
        dark: {
            ".lang-selector-option": (el) => {
                if (el.classList.contains("text-emerald-700")) {
                    el.classList.remove("text-emerald-700");
                    el.classList.add("text-emerald-300");
                }
            },
        },
    };

    function getTheme() {
        const cookie = document.cookie
                .split("; ")
                .find((row) => row.startsWith("theme="));
        return cookie ? cookie.split("=")[1] : "dark";
    }

    function setThemeCookie(theme) {
        document.cookie =
                "theme=" + theme + "; path=/; max-age=31536000; SameSite=Lax";
    }

    function applyTheme(theme) {
        const config = themeConfig[theme];
        const handlers = specialHandlers[theme] || {};

        // Apply theme classes based on configuration
        for (const [selector, classes] of Object.entries(config)) {
            let elements;
            if (selector === "html") {
                elements = [html];
            } else if (selector === "body") {
                elements = [body];
            } else {
                elements = document.querySelectorAll(selector);
            }

            elements.forEach((el) => {
                if (classes.remove) {
                    el.classList.remove(...classes.remove);
                }
                if (classes.add) {
                    el.classList.add(...classes.add);
                }

                // Apply special handler if exists
                if (handlers[selector]) {
                    handlers[selector](el);
                }
            });
        }

        // Toggle theme icons
        if (theme === "light") {
            if (darkIcon)
                darkIcon.classList.add("hidden");
            if (lightIcon)
                lightIcon.classList.remove("hidden");
        } else {
            if (darkIcon)
                darkIcon.classList.remove("hidden");
            if (lightIcon)
                lightIcon.classList.add("hidden");
        }
    }

    // apply theme on load
    const currentTheme = getTheme();
    applyTheme(currentTheme);

    // toggle handler
    if (themeToggle) {
        themeToggle.addEventListener("click", function () {
            const current = getTheme();
            const newTheme = current === "dark" ? "light" : "dark";
            setThemeCookie(newTheme);
            applyTheme(newTheme);
        });
    }

    // re-apply theme after dynamic content is added (for item rows etc)
    const observer = new MutationObserver(() => {
        applyTheme(getTheme());
    });
    observer.observe(document.body, {childList: true, subtree: true});
})();
