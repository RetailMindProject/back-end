package com.example.back_end.modules.sales.order.controller;

import com.example.back_end.modules.sales.order.dto.OrderDTO;
import com.example.back_end.modules.sales.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for POS order operations
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create new order
     * POST /api/orders
     */
    @PostMapping
    public ResponseEntity<OrderDTO.OrderResponse> createOrder(
            @Valid @RequestBody OrderDTO.CreateRequest request) {
        OrderDTO.OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Add item to order
     * POST /api/orders/items
     */
    @PostMapping("/items")
    public ResponseEntity<OrderDTO.OrderResponse> addItem(
            @Valid @RequestBody OrderDTO.AddItemRequest request) {
        OrderDTO.OrderResponse response = orderService.addItem(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update item quantity (increment)
     * PUT /api/orders/items
     *
     * ملاحظة:
     * - request.quantity هنا تمثل مقدار الزيادة (delta) على الكمية الحالية.
     * - الحذف يتم عبر DELETE /api/orders/items/{itemId}
     */
    @PutMapping("/items")
    public ResponseEntity<OrderDTO.OrderResponse> updateItemQuantity(
            @Valid @RequestBody OrderDTO.UpdateItemRequest request) {

        // المنطق كله في السيرفس: يتحقق من أن الكمية > 0 ويعمل increment
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
    public ResponseEntity<OrderDTO.OrderResponse> processPayment(
            @Valid @RequestBody OrderDTO.PaymentRequest request) {
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
     * Get draft/held orders for session
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
     * Hold order (save for later)
     * POST /api/orders/{id}/hold
     */
    @PostMapping("/{id}/hold")
    public ResponseEntity<OrderDTO.OrderResponse> holdOrder(@PathVariable Long id) {
        OrderDTO.OrderResponse response = orderService.holdOrder(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve held order
     * POST /api/orders/{id}/retrieve
     */
    @PostMapping("/{id}/retrieve")
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
