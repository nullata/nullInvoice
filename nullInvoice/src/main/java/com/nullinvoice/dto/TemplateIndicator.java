// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Indicates which template is effective and its source.
 */
@Getter
@AllArgsConstructor
public class TemplateIndicator {
    private final String templateName;
    private final String sourceKey; // "supplier" or "global"
}
