// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStats {

    private long invoiceCount;
    private long supplierCount;
    private long clientCount;
    private long templateCount;
}
