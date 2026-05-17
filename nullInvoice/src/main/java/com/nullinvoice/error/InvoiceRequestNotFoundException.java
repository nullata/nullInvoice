// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.error;

public class InvoiceRequestNotFoundException extends RuntimeException {

    public InvoiceRequestNotFoundException(Long requestId) {
        super("invoice request not found: " + requestId);
    }
}
