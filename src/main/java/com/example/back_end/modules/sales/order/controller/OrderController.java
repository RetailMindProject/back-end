package com.example.back_end.modules.sales.order.controller;

import com.example.back_end.common.dto.BrowserContext;
import com.example.back_end.common.filter.BrowserTokenFilter;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.service.SessionLifecycleService;
import com.example.back_end.modules.sales.order.dto.OrderDTO;
import com.example.back_end.modules.sales.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for POS order operations
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final SessionLifecycleService lifecycleService;

    /**
     * Create new order
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderDTO.CreateRequest request,
            HttpServletRequest httpRequest) {

        try {
            BrowserContext context = BrowserTokenFilter.getContext(httpRequest);

            if (context == null || !context.isPaired()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No terminal is paired with this browser"));
            }

            // ✅ READ ONLY - do not create session
            Session session = lifecycleService.getCurrentSession(context.getTerminalId());

            if (session == null || !Session.SessionStatus.OPEN.equals(session.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "No open session. Please start a session first."));
            }

            request.setSessionId(session.getId());

            OrderDTO.OrderResponse response = orderService.createOrder(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create order: " + e.getMessage()));
        }
    }


    /**
     * Add item to order
     * POST /api/orders/items
     */
    @PostMapping("/items")
    public ResponseEntity<?> addItem(
            @Valid @RequestBody OrderDTO.AddItemRequest request,
            HttpServletRequest httpRequest) {

        // ✅ Verify browser is paired (optional - for safety)
        BrowserContext context = BrowserTokenFilter.getContext(httpRequest);
        if (context == null || !context.isPaired()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No terminal is paired"));
        }

        OrderDTO.OrderResponse response = orderService.addItem(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update item quantity (increment)
     * PUT /api/orders/items
     */
    @PutMapping("/items")
    public ResponseEntity<OrderDTO.OrderResponse> updateItemQuantity(
            @Valid @RequestBody OrderDTO.UpdateItemRequest request) {
        OrderDTO.OrderResponse response = orderService.updateItemQuantity(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove item from order
     * DELETE /api/orders/items/{itemId}
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<OrderDTO.OrderResponse> removeItem(@PathVariable Long itemId) {
        OrderDTO.OrderResponse response = orderService.removeItem(itemId);
        return ResponseEntity.ok(response);
    }

    /**
     * Apply discount to order
     * POST /api/orders/discount
     */
    @PostMapping("/discount")
    public ResponseEntity<OrderDTO.OrderResponse> applyDiscount(
            @Valid @RequestBody OrderDTO.ApplyDiscountRequest request) {
        OrderDTO.OrderResponse response = orderService.applyDiscount(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Process payment
     * POST /api/orders/payment
     * POST /api/orders/payments (alias)
     */
    @PostMapping({"/payment", "/payments"})
    public ResponseEntity<?> processPayment(
            @Valid @RequestBody OrderDTO.PaymentRequest request,
            HttpServletRequest httpRequest) {

        // ✅ Verify browser is paired
        BrowserContext context = BrowserTokenFilter.getContext(httpRequest);
        if (context == null || !context.isPaired()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No terminal is paired"));
        }

        // ✅ READ ONLY - must have open session
        Session session = lifecycleService.getCurrentSession(context.getTerminalId());

        if (session == null || !Session.SessionStatus.OPEN.equals(session.getStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No open session. Please start a session first."));
        }

        OrderDTO.OrderResponse response = orderService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get order history for session
     * GET /api/orders/session/{sessionId}/history
     */
    @GetMapping("/session/{sessionId}/history")
    public ResponseEntity<List<OrderDTO.OrderSummary>> getSessionHistory(@PathVariable Long sessionId) {
        List<OrderDTO.OrderSummary> history = orderService.getSessionOrderHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    /**
     * Get draft/hold orders for session
     * GET /api/orders/session/{sessionId}/drafts
     */
    @GetMapping("/session/{sessionId}/drafts")
    public ResponseEntity<List<OrderDTO.OrderSummary>> getDraftOrders(@PathVariable Long sessionId) {
        List<OrderDTO.OrderSummary> drafts = orderService.getDraftOrders(sessionId);
        return ResponseEntity.ok(drafts);
    }

    /**
     * Get order details
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO.OrderResponse> getOrder(@PathVariable Long id) {
        OrderDTO.OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Hold order
     * PUT /api/orders/{id}/hold
     */
    @PutMapping("/{id}/hold")
    public ResponseEntity<OrderDTO.OrderResponse> holdOrder(@PathVariable Long id) {
        OrderDTO.OrderResponse response = orderService.holdOrder(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve held order
     * PUT /api/orders/{id}/retrieve
     */
    @PutMapping("/{id}/retrieve")
    public ResponseEntity<OrderDTO.OrderResponse> retrieveOrder(@PathVariable Long id) {
        OrderDTO.OrderResponse response = orderService.retrieveOrder(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Void/delete order
     * DELETE /api/orders/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> voidOrder(@PathVariable Long id) {
        orderService.voidOrder(id);
        return ResponseEntity.noContent().build();
    }
}