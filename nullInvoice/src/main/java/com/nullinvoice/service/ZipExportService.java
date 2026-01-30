// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.nullinvoice.entity.Invoices;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZipExportService {

    private final PdfService pdfService;
    private final TemplateService templateService;
    private final InvoiceService invoiceService;

    /**
     * Writes a ZIP archive containing PDF files for each invoice to the given output stream.
     * If any PDFs fail to generate, an _export_errors.txt file is included in the ZIP
     */
    public void writeInvoicesZip(List<Invoices> invoices, OutputStream outputStream) {
        List<String> errors = new ArrayList<>();

        try (ZipOutputStream zipOut = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            for (Invoices invoice : invoices) {
                String filename = invoiceService.formatInvoiceFilename(invoice);
                try {
                    String html = templateService.resolveInvoiceHtml(invoice);
                    byte[] pdfBytes = pdfService.renderPdf(html);

                    ZipEntry entry = new ZipEntry(filename);
                    zipOut.putNextEntry(entry);
                    zipOut.write(pdfBytes);
                    zipOut.closeEntry();
                } catch (IOException e) {
                    log.error("Failed to generate PDF for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage(), e);
                    errors.add(formatError(invoice, e));
                }
            }

            if (!errors.isEmpty()) {
                writeErrorsFile(zipOut, errors);
            }
        } catch (Exception e) {
            log.error("Failed to create ZIP export: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to create ZIP export", e);
        }
    }

    private String formatError(Invoices invoice, Exception e) {
        StringWriter sw = new StringWriter();
        sw.append("Invoice: ").append(invoice.getInvoiceNumber()).append("\n");
        sw.append("Error: ").append(e.getMessage()).append("\n");
        if (e.getCause() != null) {
            sw.append("Cause: ").append(e.getCause().getMessage()).append("\n");
        }
        sw.append("\n");
        return sw.toString();
    }

    private void writeErrorsFile(ZipOutputStream zipOut, List<String> errors) {
        try {
            ZipEntry errorEntry = new ZipEntry("_export_errors.txt");
            zipOut.putNextEntry(errorEntry);

            PrintWriter writer = new PrintWriter(zipOut, false, StandardCharsets.UTF_8);
            writer.println("The following invoices failed to export:");
            writer.println("==========================================");
            writer.println();
            for (String error : errors) {
                writer.print(error);
            }
            writer.flush();
            zipOut.closeEntry();
        } catch (IOException e) {
            log.error("Failed to write errors file to ZIP: {}", e.getMessage(), e);
        }
    }
}
