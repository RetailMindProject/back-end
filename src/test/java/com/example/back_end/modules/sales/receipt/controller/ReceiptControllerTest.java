package com.example.back_end.modules.sales.receipt.controller;

import com.example.back_end.common.dto.BrowserContext;
import com.example.back_end.common.filter.BrowserTokenFilter;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.service.SessionLifecycleService;
import com.example.back_end.modules.sales.receipt.service.ReceiptService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReceiptControllerTest {

    @Test
    void shouldReturnPdf_whenPaid() {
        ReceiptService receiptService = mock(ReceiptService.class);
        SessionLifecycleService lifecycleService = mock(SessionLifecycleService.class);

        ReceiptController controller = new ReceiptController(receiptService, lifecycleService);

        HttpServletRequest request = mock(HttpServletRequest.class);

        BrowserContext ctx = BrowserContext.builder()
                .isPaired(true)
                .terminalId(1L)
                .browserTokenHash("hash")
                .build();
        when(request.getAttribute(BrowserTokenFilter.BROWSER_CONTEXT_ATTRIBUTE)).thenReturn(ctx);

        Session session = new Session();
        session.setStatus(Session.SessionStatus.OPEN);
        when(lifecycleService.getCurrentSession(1L)).thenReturn(session);

        byte[] pdf = "%PDF-test".getBytes();
        when(receiptService.generateReceiptPdf(10L)).thenReturn(pdf);

        ResponseEntity<?> resp = controller.getReceiptPdf(10L, request);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getHeaders().getContentType()).isNotNull();
        assertThat(resp.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat((byte[]) resp.getBody()).isEqualTo(pdf);
    }

    @Test
    void shouldReturn409_whenNotPaid() {
        ReceiptService receiptService = mock(ReceiptService.class);
        SessionLifecycleService lifecycleService = mock(SessionLifecycleService.class);

        ReceiptController controller = new ReceiptController(receiptService, lifecycleService);
        HttpServletRequest request = mock(HttpServletRequest.class);

        BrowserContext ctx = BrowserContext.builder()
                .isPaired(true)
                .terminalId(1L)
                .browserTokenHash("hash")
                .build();
        when(request.getAttribute(BrowserTokenFilter.BROWSER_CONTEXT_ATTRIBUTE)).thenReturn(ctx);

        Session session = new Session();
        session.setStatus(Session.SessionStatus.OPEN);
        when(lifecycleService.getCurrentSession(1L)).thenReturn(session);

        when(receiptService.generateReceiptPdf(10L))
                .thenThrow(new IllegalStateException("Receipt can only be generated for PAID orders"));

        ResponseEntity<?> resp = controller.getReceiptPdf(10L, request);

        assertThat(resp.getStatusCode().value()).isEqualTo(409);
        assertThat(resp.getBody()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) resp.getBody()).get("error")).isNotNull();
        assertThat(((Map<?, ?>) resp.getBody()).get("error").toString()).contains("PAID");
    }
}
