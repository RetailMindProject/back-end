package com.example.back_end.modules.sales.returns.controller;

import com.example.back_end.common.dto.BrowserContext;
import com.example.back_end.common.filter.BrowserTokenFilter;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.service.SessionLifecycleService;
import com.example.back_end.modules.sales.returns.dto.ReturnDTO;
import com.example.back_end.modules.sales.returns.dto.ReturnHistoryDTO;
import com.example.back_end.modules.sales.returns.service.ReturnHistoryService;
import com.example.back_end.modules.sales.returns.service.ReturnService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;
    private final SessionLifecycleService lifecycleService;
    private final ReturnHistoryService returnHistoryService;

    @PostMapping
    public ResponseEntity<?> createReturn(
            @Valid @RequestBody ReturnDTO.CreateReturnRequest request,
            HttpServletRequest httpRequest) {

        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null || !context.isPaired()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No terminal is paired with this browser"));
            }

            // ✅ MUST have OPEN session for this paired terminal
            Session session = lifecycleService.getCurrentSession(context.getTerminalId());

            if (session == null || !Session.SessionStatus.OPEN.equals(session.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No open session. Please start a session first."));
            }

            // ✅ Consistent rule: ignore provided sessionId and always use current open session
            request.setSessionId(session.getId());

            ReturnDTO.ReturnResponse response = returnService.createReturn(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create return: " + e.getMessage()));
        }
    }

    /**
     * Frontend debugging helper.
     * Some clients call GET /api/returns accidentally; returns flow uses POST /api/returns.
     */
    @GetMapping
    public ResponseEntity<?> returnsRoot() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of(
                        "error", "METHOD_NOT_ALLOWED",
                        "message", "Use POST /api/returns to create a return",
                        "path", "/api/returns"
                ));
    }

    /**
     * A) List original orders that have returns.
     * GET /api/returns/orders?limit=10&offset=0&from=...&to=...&q=...
     */
    @GetMapping("/orders")
    public ResponseEntity<ReturnHistoryDTO.ReturnedOrdersPage> listReturnedOrders(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) java.time.LocalDate from,
            @RequestParam(required = false) java.time.LocalDate to,
            @RequestParam(required = false) String q) {

        ReturnHistoryDTO.ReturnedOrdersPage page = returnHistoryService.listReturnedOrders(limit, offset, from, to, q);
        return ResponseEntity.ok(page);
    }

    /**
     * C) Return details for a specific return order.
     * GET /api/returns/{returnOrderId}
     */
    @GetMapping("/{returnOrderId}")
    public ResponseEntity<ReturnHistoryDTO.ReturnDetails> getReturnDetails(@PathVariable Long returnOrderId) {
        return ResponseEntity.ok(returnHistoryService.getReturnDetails(returnOrderId));
    }
}
