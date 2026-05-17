// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceRequestResponse {

    private Long requestId;
    private String status;
    private String message;
    private Long invoiceId;
    private String invoiceNumber;
    private String errorMessage;
    private Integer attempts;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
