// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApiKeyDto {

    private Long id;
    private String keySuffix;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
