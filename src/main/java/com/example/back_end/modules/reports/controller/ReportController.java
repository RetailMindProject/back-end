package com.example.back_end.modules.reports.controller;

import com.example.back_end.modules.reports.dto.ReportMetaDTO;
import com.example.back_end.modules.reports.dto.ReportSummaryDTO;
import com.example.back_end.modules.reports.dto.TimeSeriesDTO;
import com.example.back_end.modules.reports.service.ReportService;
import com.example.back_end.modules.sales.order.dto.OrderDTO;
import com.example.back_end.modules.sales.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;
    private final OrderService orderService;

    /**
     * Get sales summary/KPIs
     * GET /api/reports/summary?from=2025-01-01&to=2025-01-31
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER')")
    public ResponseEntity<ReportSummaryDTO> getSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);

        ReportSummaryDTO summary = reportService.getSummary(fromDate, toDate);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get time series data
     * GET /api/reports/series?from=2025-01-01&to=2025-01-31
     */
    @GetMapping("/series")
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER')")
    public ResponseEntity<List<TimeSeriesDTO>> getTimeSeries(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);

        List<TimeSeriesDTO> series = reportService.getTimeSeries(fromDate, toDate);
        return ResponseEntity.ok(series);
    }

    /**
     * Get filter metadata (cashiers, statuses, date ranges)
     * GET /api/reports/meta
     */
    @GetMapping("/meta")
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER')")
    public ResponseEntity<ReportMetaDTO> getMeta() {
        ReportMetaDTO meta = reportService.getMeta();
        return ResponseEntity.ok(meta);
    }

    /**
     * Export order report data as CSV
     * GET /api/reports/export?from=2025-01-01&to=2025-01-31&format=csv
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('CEO', 'STORE_MANAGER')")
    public ResponseEntity<String> exportReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String cashierName,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false, defaultValue = "PAID") String status,
            @RequestParam(required = false, defaultValue = "csv") String format,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset
    ) {
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);

        // Get order report data
        OrderDTO.OrderReportResponse reportResponse = orderService.getOrderReport(
                fromDate,
                toDate,
                cashierName,
                cashierId,
                status,
                limit != null ? limit : 10000, // Large limit for export
                offset != null ? offset : 0,
                "date",
                "DESC"
        );

        // Convert to CSV
        String csv = convertToCsv(reportResponse.getItems());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "order-report.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    private String convertToCsv(List<OrderDTO.OrderReportItem> items) {
        StringBuilder csv = new StringBuilder();
        
        // CSV Header
        csv.append("Order ID,Order Number,Date,Time,Session ID,Cashier ID,Cashier Name,")
           .append("Customer Name,Customer Phone,Status,Item Count,Subtotal,")
           .append("Discount Amount,Tax Amount,Grand Total,Payment Method,Paid At,Created At\n");

        // CSV Rows
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (OrderDTO.OrderReportItem item : items) {
            csv.append(escapeCsv(item.getOrderId() != null ? item.getOrderId().toString() : "")).append(",")
               .append(escapeCsv(item.getOrderNumber())).append(",")
               .append(escapeCsv(item.getDate())).append(",")
               .append(escapeCsv(item.getTime())).append(",")
               .append(escapeCsv(item.getSessionId() != null ? item.getSessionId().toString() : "")).append(",")
               .append(escapeCsv(item.getCashierId() != null ? item.getCashierId().toString() : "")).append(",")
               .append(escapeCsv(item.getCashierName())).append(",")
               .append(escapeCsv(item.getCustomerName())).append(",")
               .append(escapeCsv(item.getCustomerPhone())).append(",")
               .append(escapeCsv(item.getStatus())).append(",")
               .append(escapeCsv(item.getItemCount() != null ? item.getItemCount().toString() : "")).append(",")
               .append(escapeCsv(item.getSubtotal() != null ? item.getSubtotal().toString() : "")).append(",")
               .append(escapeCsv(item.getDiscountAmount() != null ? item.getDiscountAmount().toString() : "")).append(",")
               .append(escapeCsv(item.getTaxAmount() != null ? item.getTaxAmount().toString() : "")).append(",")
               .append(escapeCsv(item.getGrandTotal() != null ? item.getGrandTotal().toString() : "")).append(",")
               .append(escapeCsv(item.getPaymentMethod())).append(",")
               .append(escapeCsv(item.getPaidAt() != null ? item.getPaidAt().format(dateTimeFormatter) : "")).append(",")
               .append(escapeCsv(item.getCreatedAt() != null ? item.getCreatedAt().format(dateTimeFormatter) : ""))
               .append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
