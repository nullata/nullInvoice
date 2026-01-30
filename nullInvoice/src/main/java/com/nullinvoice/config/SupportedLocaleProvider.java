// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class SupportedLocaleProvider {

    private static final String BUNDLE_PREFIX = "messages_";
    private static final String BUNDLE_SUFFIX = ".properties";
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final List<LocaleOption> supportedLocales;

    public SupportedLocaleProvider() {
        Set<String> suffixes = new HashSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath*:messages_*.properties");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || !filename.startsWith(BUNDLE_PREFIX) || !filename.endsWith(BUNDLE_SUFFIX)) {
                    continue;
                }
                String suffix = filename.substring(BUNDLE_PREFIX.length(), filename.length() - BUNDLE_SUFFIX.length());
                if (!suffix.isBlank()) {
                    // turn messages_bg.properties into language tag "bg" for Locale lookup
                    suffixes.add(suffix.replace('_', '-'));
                }
            }
        } catch (IOException ignored) {
            // fall back to default locale only
        }

        List<String> ordered = new ArrayList<>();
        ordered.add(DEFAULT_LOCALE.toLanguageTag());
        suffixes.stream()
                .filter(code -> !code.equalsIgnoreCase(DEFAULT_LOCALE.toLanguageTag()))
                .sorted(Comparator.naturalOrder())
                .forEach(ordered::add);

        List<LocaleOption> options = new ArrayList<>();
        for (String code : ordered) {
            Locale locale = Locale.forLanguageTag(code);
            // use the locale display name (eg, Bulgarian, English, Spanish) derived from the language tag
            String label = locale.getDisplayName(Locale.ENGLISH);
            if (label == null || label.isBlank()) {
                label = code.toUpperCase(Locale.ROOT);
            }
            options.add(new LocaleOption(code, label));
        }
        this.supportedLocales = List.copyOf(options);
    }

    public List<LocaleOption> getSupportedLocales() {
        return supportedLocales;
    }
}
