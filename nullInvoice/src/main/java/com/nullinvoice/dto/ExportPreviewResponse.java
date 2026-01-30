// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportPreviewResponse {

    private long count;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate earliestDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate latestDate;

    private boolean exceedsLimit;
    private int limit;
}
