// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.config.LocaleOption;
import com.nullinvoice.config.SupportedLocaleProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SupportedLocaleProvider supportedLocaleProvider;

    @ModelAttribute("supportedLocales")
    public List<LocaleOption> supportedLocales() {
        return supportedLocaleProvider.getSupportedLocales();
    }
}
