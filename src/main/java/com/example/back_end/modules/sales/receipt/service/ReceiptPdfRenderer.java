package com.example.back_end.modules.sales.receipt.service;

import com.example.back_end.modules.sales.receipt.dto.ReceiptData;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a single-page 80mm thermal receipt PDF.
 * Width is fixed to 80mm, height is calculated based on number of lines.
 */
public class ReceiptPdfRenderer {

    private static final float MM_TO_PT = 72f / 25.4f;

    // 80mm receipt paper width
    private static final float PAGE_WIDTH_PT = 80f * MM_TO_PT;

    // Margins ~3mm
    private static final float MARGIN_PT = 3f * MM_TO_PT;

    private static final float FONT_SIZE = 9f;
    private static final float LEADING = 11f; // line height

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] render(ReceiptData data) {
        try (PDDocument document = new PDDocument()) {
            List<String> lines = buildLines(data);

            float height = calcHeight(lines.size());
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH_PT, height));
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.setFont(PDType1Font.COURIER, FONT_SIZE);

                float y = height - MARGIN_PT - FONT_SIZE;
                float x = MARGIN_PT;

                for (String line : lines) {
                    cs.beginText();
                    cs.newLineAtOffset(x, y);
                    cs.showText(safe(line));
                    cs.endText();
                    y -= LEADING;
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate receipt PDF", e);
        }
    }

    private float calcHeight(int lineCount) {
        // base padding + line height
        float base = MARGIN_PT * 2 + 60;
        return Math.max(120, base + lineCount * LEADING);
    }

    private List<String> buildLines(ReceiptData data) {
        List<String> lines = new ArrayList<>();

        // Header
        lines.add(center(data.header().storeName()));
        lines.add(center(data.header().storePhone()));
        lines.add(divider());
        lines.add("Order: " + nullSafe(data.header().orderNumber()));
        lines.add("Paid:  " + (data.header().paidAt() != null ? TIME_FMT.format(data.header().paidAt()) : "-"));
        lines.add(divider());

        // Items
        for (ReceiptData.ItemLine it : data.items()) {
            String nameLine = truncate((nullSafe(it.sku()) + " " + nullSafe(it.name())).trim(), 32);
            lines.add(nameLine);

            BigDecimal qty = nz(it.quantity());
            BigDecimal price = nz(it.unitPrice());
            BigDecimal total = nz(it.lineTotal());

            lines.add(String.format("%s x %s = %s",
                    fmtQty(qty),
                    fmtMoney(price),
                    fmtMoney(total)));
        }

        lines.add(divider());

        // Totals
        ReceiptData.Totals t = data.totals();
        lines.add(kv("Subtotal", fmtMoney(nz(t.subtotal()))));
        lines.add(kv("Discount", fmtMoney(nz(t.discountTotal()))));
        lines.add(kv("Tax", fmtMoney(nz(t.taxTotal()))));
        lines.add(kv("TOTAL", fmtMoney(nz(t.grandTotal()))));

        lines.add(divider());

        // Payments
        ReceiptData.PaymentSummary p = data.payments();
        BigDecimal cash = nz(p.cash());
        BigDecimal card = nz(p.card());
        if (cash.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(kv("CASH", fmtMoney(cash)));
        }
        if (card.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(kv("CARD", fmtMoney(card)));
        }
        if (cash.compareTo(BigDecimal.ZERO) == 0 && card.compareTo(BigDecimal.ZERO) == 0) {
            lines.add("Payments: -");
        } else if (cash.compareTo(BigDecimal.ZERO) > 0 && card.compareTo(BigDecimal.ZERO) > 0) {
            lines.add("Payment: MIXED");
        } else {
            lines.add("Payment: " + (cash.compareTo(BigDecimal.ZERO) > 0 ? "CASH" : "CARD"));
        }

        lines.add(divider());
        lines.add(center("Thank you!"));

        return lines;
    }

    private String divider() {
        return "--------------------------------";
    }

    private String kv(String k, String v) {
        // keep simple for 80mm
        return String.format("%-10s %s", k + ":", v);
    }

    private String center(String s) {
        s = nullSafe(s);
        int width = 32;
        if (s.length() >= width) return s;
        int left = (width - s.length()) / 2;
        return " ".repeat(Math.max(0, left)) + s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String safe(String s) {
        // PDFBox Type1 fonts are WinAnsi; avoid control chars
        if (s == null) return "";
        return s.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String fmtMoney(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String fmtQty(BigDecimal v) {
        if (v == null) return "0";
        BigDecimal scaled = v.stripTrailingZeros();
        return scaled.scale() < 0 ? scaled.setScale(0, RoundingMode.UNNECESSARY).toPlainString() : scaled.toPlainString();
    }
}

