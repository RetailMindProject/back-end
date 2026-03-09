package com.example.back_end.modules.sales.receipt.controller;

import com.example.back_end.common.dto.BrowserContext;
import com.example.back_end.common.filter.BrowserTokenFilter;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.service.SessionLifecycleService;
import com.example.back_end.modules.sales.receipt.service.ReceiptService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;
    private final SessionLifecycleService lifecycleService;

    @GetMapping(value = "/{orderId}/receipt.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> getReceiptPdf(@PathVariable Long orderId, HttpServletRequest httpRequest) {
        try {
            // Enforce same cashier rules as orders/payment flow
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);
            if (context == null || !context.isPaired()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No terminal is paired with this browser"));
            }

            Session session = lifecycleService.getCurrentSession(context.getTerminalId());
            if (session == null || !Session.SessionStatus.OPEN.equals(session.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No open session. Please start a session first."));
            }

            byte[] pdf = receiptService.generateReceiptPdf(orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=receipt-" + orderId + ".pdf");

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            // Not PAID
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate receipt"));
        }
    }
}

