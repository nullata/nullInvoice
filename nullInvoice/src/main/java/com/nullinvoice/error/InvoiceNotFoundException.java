// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.error;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(String invoiceNumber) {
        super("invoice not found: " + invoiceNumber);
    }
}
