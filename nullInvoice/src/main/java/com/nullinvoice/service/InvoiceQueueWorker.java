// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.entity.InvoiceRequest;
import com.nullinvoice.repository.InvoiceRequestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "nullinvoice.queue", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class InvoiceQueueWorker {

    private final InvoiceRequestRepository repository;
    private final InvoiceQueueProcessor processor;

    @Value("${nullinvoice.queue.batch-size:10}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${nullinvoice.queue.poll-interval-ms:2000}")
    public void processQueue() {
        List<InvoiceRequest> candidates = repository.findPendingForProcessing(
                PageRequest.of(0, batchSize));
        if (candidates.isEmpty()) {
            return;
        }
        for (InvoiceRequest req : candidates) {
            Long id = req.getId();
            try {
                processor.processOne(id);
            } catch (Exception ex) {
                try {
                    processor.recordFailure(id, ex);
                } catch (Exception inner) {
                    log.error("failed to record failure for invoice request {}", id, inner);
                }
            }
        }
    }
}
