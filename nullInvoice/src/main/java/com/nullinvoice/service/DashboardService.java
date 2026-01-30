// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.dto.DashboardStats;
import com.nullinvoice.repository.InvoiceRepository;
import com.nullinvoice.repository.InvoiceTemplateRepository;
import com.nullinvoice.repository.PartyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final PartyProfileRepository partyRepository;
    private final InvoiceTemplateRepository templateRepository;

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        return DashboardStats.builder()
                .invoiceCount(invoiceRepository.count())
                .supplierCount(partyRepository.countByRoleAndDeletedFalse("SUPPLIER"))
                .clientCount(partyRepository.countByRoleAndDeletedFalse("CLIENT"))
                .templateCount(templateRepository.count())
                .build();
    }
}
