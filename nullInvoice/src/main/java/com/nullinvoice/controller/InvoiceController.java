// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.controller;

import com.nullinvoice.dto.ExportPreviewResponse;
import com.nullinvoice.dto.GenerateInvoiceRequest;
import com.nullinvoice.dto.GenerateInvoiceResponse;
import com.nullinvoice.dto.InvoiceResponse;
import com.nullinvoice.entity.Invoices;
import com.nullinvoice.service.InvoiceMapper;
import com.nullinvoice.service.InvoiceService;
import com.nullinvoice.service.PdfService;
import com.nullinvoice.service.TemplateService;
import com.nullinvoice.service.ZipExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice generation and retrieval")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final TemplateService templateService;
    private final PdfService pdfService;
    private final InvoiceMapper mapper;
    private final ZipExportService zipExportService;

    private static final int EXPORT_LIMIT = 500;

    @PostMapping("/invoices/generate")
    @Operation(summary = "Generate an invoice",
            description = "Generates an invoice. Set responseType to 'pdf' to receive PDF file with metadata in response headers (X-Invoice-Number, X-Invoice-Status, X-Invoice-Issue-Date). Omit or use 'number' for JSON response only.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice generated - JSON response or PDF file depending on responseType",
                content = @Content(schema = @Schema(implementation = GenerateInvoiceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<?> generateInvoice(@Valid @RequestBody GenerateInvoiceRequest request) {
        Invoices invoice = invoiceService.generateInvoice(request);
        GenerateInvoiceResponse response = GenerateInvoiceResponse.builder()
                .status(invoice.getStatus())
                .message("invoice generated")
                .invoiceNumber(invoice.getInvoiceNumber())
                .issueDate(mapper.toLocalDate(invoice.getIssueDate()))
                .build();

        String responseType = request.getResponseType() == null ? "number" : request.getResponseType().toLowerCase(Locale.ROOT);
        if (responseType.isBlank() || responseType.equals("number")) {
            return ResponseEntity.ok(response);
        }
        if (!responseType.equals("pdf")) {
            throw new IllegalArgumentException("response_type must be 'number' or 'pdf'");
        }

        String html = templateService.resolveInvoiceHtml(invoice);
        byte[] pdfBytes = pdfService.renderPdf(html);
        String filename = invoiceService.formatInvoiceFilename(invoice);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .header("X-Invoice-Number", invoice.getInvoiceNumber())
                .header("X-Invoice-Status", invoice.getStatus())
                .header("X-Invoice-Issue-Date", mapper.toLocalDate(invoice.getIssueDate()).toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/invoices/{invoiceNumber}")
    @Operation(summary = "Get invoice by number")
    public InvoiceResponse getInvoice(
            @Parameter(description = "Invoice number", required = true)
            @PathVariable String invoiceNumber,
            @Parameter(description = "Supplier id for disambiguation")
            @RequestParam(name = "supplierId", required = false) Long supplierId) {
        Invoices invoice = invoiceService.getInvoiceByNumber(invoiceNumber, supplierId);
        return mapper.toResponse(invoice);
    }

    @GetMapping("/invoices")
    @Operation(summary = "List invoices", description = "Optional status filter (issued/unpaid).")
    public ResponseEntity<List<InvoiceResponse>> listInvoices(
            @Parameter(description = "Filter by invoice status", example = "unpaid")
            @RequestParam(name = "status", required = false) String status) {
        List<Invoices> invoices = invoiceService.listInvoicesByStatus(status);
        List<InvoiceResponse> response = invoices.stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/invoices/{invoiceNumber}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download invoice PDF")
    public ResponseEntity<byte[]> getInvoicePdf(
            @Parameter(description = "Invoice number", required = true)
            @PathVariable String invoiceNumber,
            @Parameter(description = "Supplier id for disambiguation")
            @RequestParam(name = "supplierId", required = false) Long supplierId) {
        Invoices invoice = invoiceService.getInvoiceByNumber(invoiceNumber, supplierId);
        String html = templateService.resolveInvoiceHtml(invoice);
        byte[] pdfBytes = pdfService.renderPdf(html);

        String filename = invoiceService.formatInvoiceFilename(invoice);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(pdfBytes);
    }

    @GetMapping("/invoices/export/preview")
    @Operation(summary = "Preview bulk export", description = "Returns count and date boundaries for a supplier's invoices")
    public ResponseEntity<ExportPreviewResponse> exportPreview(
            @Parameter(description = "Supplier ID", required = true, example = "1")
            @RequestParam(name = "supplierId") Long supplierId,
            @Parameter(description = "Start date (inclusive)", example = "2025-01-01")
            @RequestParam(name = "startDate", required = false) LocalDate startDate,
            @Parameter(description = "End date (inclusive)", example = "2025-12-31")
            @RequestParam(name = "endDate", required = false) LocalDate endDate) {

        Date earliestDate = invoiceService.getEarliestDate(supplierId);
        Date latestDate = invoiceService.getLatestDate(supplierId);

        if (earliestDate == null || latestDate == null) {
            return ResponseEntity.ok(ExportPreviewResponse.builder()
                    .count(0)
                    .earliestDate(null)
                    .latestDate(null)
                    .exceedsLimit(false)
                    .limit(EXPORT_LIMIT)
                    .build());
        }

        Date effectiveStart = startDate != null ? toDate(startDate) : earliestDate;
        Date effectiveEnd = endDate != null ? toDate(endDate) : latestDate;

        long count = invoiceService.countInvoicesByDateRange(supplierId, effectiveStart, effectiveEnd);

        return ResponseEntity.ok(ExportPreviewResponse.builder()
                .count(count)
                .earliestDate(mapper.toLocalDate(earliestDate))
                .latestDate(mapper.toLocalDate(latestDate))
                .exceedsLimit(count > EXPORT_LIMIT)
                .limit(EXPORT_LIMIT)
                .build());
    }

    @GetMapping(value = "/invoices/export/zip", produces = "application/zip")
    @Operation(summary = "Export invoices as ZIP", description = "Downloads a ZIP file containing PDFs for invoices in date range")
    public ResponseEntity<StreamingResponseBody> exportZip(
            @Parameter(description = "Supplier ID", required = true, example = "1")
            @RequestParam(name = "supplierId") Long supplierId,
            @Parameter(description = "Start date (inclusive)", example = "2025-01-01")
            @RequestParam(name = "startDate", required = false) LocalDate startDate,
            @Parameter(description = "End date (inclusive)", example = "2025-12-31")
            @RequestParam(name = "endDate", required = false) LocalDate endDate) {

        Date earliestDate = invoiceService.getEarliestDate(supplierId);
        Date latestDate = invoiceService.getLatestDate(supplierId);

        if (earliestDate == null || latestDate == null) {
            throw new IllegalArgumentException("No invoices found for supplier");
        }

        Date effectiveStart = startDate != null ? toDate(startDate) : earliestDate;
        Date effectiveEnd = endDate != null ? toDate(endDate) : latestDate;

        long count = invoiceService.countInvoicesByDateRange(supplierId, effectiveStart, effectiveEnd);
        if (count > EXPORT_LIMIT) {
            throw new IllegalArgumentException("Export limit exceeded. Maximum " + EXPORT_LIMIT + " invoices allowed.");
        }

        List<Invoices> invoices = invoiceService.findInvoicesByDateRange(supplierId, effectiveStart, effectiveEnd);

        String filename = "invoices_export_" + (startDate != null ? startDate : mapper.toLocalDate(earliestDate))
                + "_to_" + (endDate != null ? endDate : mapper.toLocalDate(latestDate)) + ".zip";

        StreamingResponseBody body = outputStream -> zipExportService.writeInvoicesZip(invoices, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(body);
    }

    private Date toDate(LocalDate value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
