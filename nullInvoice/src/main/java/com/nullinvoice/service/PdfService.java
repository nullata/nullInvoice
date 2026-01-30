// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
package com.nullinvoice.service;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    public byte[] renderPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            registerFonts(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            log.error("PDF render failed. HTML length: {}, First 200 chars: {}",
                    html.length(), html.substring(0, Math.min(200, html.length())));
            throw new IllegalStateException("failed to render pdf", ex);
        }
    }

    private void registerFonts(PdfRendererBuilder builder) {
        // DejaVu Sans
        registerFont(builder, "DejaVuSans.ttf", "DejaVu Sans", 400, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSans-Bold.ttf", "DejaVu Sans", 700, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSans-Oblique.ttf", "DejaVu Sans", 400, FontStyle.OBLIQUE);
        registerFont(builder, "DejaVuSans-BoldOblique.ttf", "DejaVu Sans", 700, FontStyle.OBLIQUE);
        registerFont(builder, "DejaVuSans-ExtraLight.ttf", "DejaVu Sans", 200, FontStyle.NORMAL);

        // DejaVu Sans Condensed
        registerFont(builder, "DejaVuSansCondensed.ttf", "DejaVu Sans Condensed", 400, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSansCondensed-Bold.ttf", "DejaVu Sans Condensed", 700, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSansCondensed-Oblique.ttf", "DejaVu Sans Condensed", 400, FontStyle.OBLIQUE);
        registerFont(builder, "DejaVuSansCondensed-BoldOblique.ttf", "DejaVu Sans Condensed", 700, FontStyle.OBLIQUE);

        // DejaVu Sans Mono
        registerFont(builder, "DejaVuSansMono.ttf", "DejaVu Sans Mono", 400, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSansMono-Bold.ttf", "DejaVu Sans Mono", 700, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSansMono-Oblique.ttf", "DejaVu Sans Mono", 400, FontStyle.OBLIQUE);
        registerFont(builder, "DejaVuSansMono-BoldOblique.ttf", "DejaVu Sans Mono", 700, FontStyle.OBLIQUE);

        // DejaVu Serif
        registerFont(builder, "DejaVuSerif.ttf", "DejaVu Serif", 400, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSerif-Bold.ttf", "DejaVu Serif", 700, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSerif-Italic.ttf", "DejaVu Serif", 400, FontStyle.ITALIC);
        registerFont(builder, "DejaVuSerif-BoldItalic.ttf", "DejaVu Serif", 700, FontStyle.ITALIC);

        // DejaVu Serif Condensed
        registerFont(builder, "DejaVuSerifCondensed.ttf", "DejaVu Serif Condensed", 400, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSerifCondensed-Bold.ttf", "DejaVu Serif Condensed", 700, FontStyle.NORMAL);
        registerFont(builder, "DejaVuSerifCondensed-Italic.ttf", "DejaVu Serif Condensed", 400, FontStyle.ITALIC);
        registerFont(builder, "DejaVuSerifCondensed-BoldItalic.ttf", "DejaVu Serif Condensed", 700, FontStyle.ITALIC);
    }

    private void registerFont(PdfRendererBuilder builder, String filename, String family, int weight, FontStyle style) {
        String resourcePath = "/fonts/" + filename;
        try (InputStream fontStream = getClass().getResourceAsStream(resourcePath)) {
            if (fontStream == null) {
                log.warn("Font not found: {}", resourcePath);
                return;
            }
            builder.useFont(() -> getClass().getResourceAsStream(resourcePath), family, weight, style, true);
        } catch (Exception e) {
            log.warn("Failed to register font {}: {}", resourcePath, e.getMessage());
        }
    }
}
