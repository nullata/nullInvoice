// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateForm {

    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String html;

    private boolean defaultTemplate;
}
