package com.example.back_end.modules.sales.returns.service;

import com.example.back_end.exception.BusinessRuleException;
import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.repository.SessionRepository;
import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.entity.OrderItem;
import com.example.back_end.modules.sales.order.repository.OrderItemRepository;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import com.example.back_end.modules.sales.payment.entity.Payment;
import com.example.back_end.modules.sales.payment.repository.PaymentRepository;
import com.example.back_end.modules.sales.returns.config.ReturnProperties;
import com.example.back_end.modules.sales.returns.dto.ReturnDTO;
import com.example.back_end.modules.sales.returns.entity.ReturnItem;
import com.example.back_end.modules.sales.returns.repository.ReturnItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnItemRepository returnItemRepository;
    private final PaymentRepository paymentRepository;
    private final SessionRepository sessionRepository;
    private final ReturnProperties returnProperties;

    @Transactional
    public ReturnDTO.ReturnResponse createReturn(ReturnDTO.CreateReturnRequest request) {
        Order originalOrder = orderRepository.findById(request.getOriginalOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Original order not found"));

        validateOriginalOrderEligible(originalOrder);

        // Validate session exists (no auto-create)
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Load original order items
        List<OrderItem> originalItems = orderItemRepository.findByOrderId(originalOrder.getId());
        Map<Long, OrderItem> originalItemById = new HashMap<>();
        for (OrderItem oi : originalItems) {
            originalItemById.put(oi.getId(), oi);
        }

        if (originalItems.isEmpty()) {
            throw new BusinessRuleException("Original order has no items");
        }

        // Validate request items and compute refunds using snapshot (lineTotal/quantity)
        List<ReturnItem> returnItemsToSave = new ArrayList<>();
        List<ReturnDTO.ReturnedItemResponse> returnedItemResponses = new ArrayList<>();

        BigDecimal totalRefund = BigDecimal.ZERO;

        for (ReturnDTO.ReturnItemRequest itemReq : request.getItems()) {
            OrderItem originalItem = originalItemById.get(itemReq.getOriginalOrderItemId());
            if (originalItem == null) {
                throw new BusinessRuleException("Order item does not belong to original order: " + itemReq.getOriginalOrderItemId());
            }

            BigDecimal returnedQty = normalizeQty(itemReq.getReturnedQty());
            if (returnedQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("returnedQty must be > 0");
            }

            BigDecimal remainingQty = computeRemainingQty(originalOrder.getId(), originalItem);
            if (returnedQty.compareTo(remainingQty) > 0) {
                throw new BusinessRuleException("Returned quantity exceeds remaining quantity for item " + originalItem.getId() +
                        ". remaining=" + remainingQty + ", requested=" + returnedQty);
            }

            BigDecimal refundAmount = computeRefundAmount(originalItem, returnedQty);
            totalRefund = totalRefund.add(refundAmount);

            ReturnItem ri = ReturnItem.builder()
                    .originalOrderItem(originalItem)
                    .returnedQty(returnedQty)
                    .refundAmount(refundAmount)
                    .build();
            returnItemsToSave.add(ri);

            returnedItemResponses.add(ReturnDTO.ReturnedItemResponse.builder()
                    .originalOrderItemId(originalItem.getId())
                    .returnedQty(returnedQty)
                    .refundAmount(refundAmount)
                    .build());
        }

        totalRefund = totalRefund.setScale(MONEY_SCALE, MONEY_ROUNDING);

        // Validate refund payments sum == totalRefund
        BigDecimal paymentsSum = request.getRefunds().stream()
                .map(r -> r.getAmount() == null ? BigDecimal.ZERO : r.getAmount())
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b))
                .setScale(MONEY_SCALE, MONEY_ROUNDING);

        if (paymentsSum.compareTo(totalRefund) != 0) {
            throw new BusinessRuleException("Refund payments total (" + paymentsSum + ") must equal computed totalRefund (" + totalRefund + ")");
        }

        // Create return order (NEW order)
        Order returnOrder = new Order();
        returnOrder.setOrderNumber(generateReturnOrderNumber(originalOrder.getOrderNumber()));
        returnOrder.setSession(session);
        returnOrder.setParentOrderId(originalOrder.getId());
        // Hard rule: auto-copy customer_id, can be null
        returnOrder.setCustomerId(originalOrder.getCustomerId());

        // Hard rule: status must be RETURNED (NOT PAID)
        returnOrder.setStatus(Order.OrderStatus.RETURNED);
        returnOrder.setPaidAt(null);

        // All totals are POSITIVE and represent refunded amounts
        returnOrder.setSubtotal(totalRefund);
        returnOrder.setDiscountTotal(BigDecimal.ZERO);
        returnOrder.setTaxTotal(BigDecimal.ZERO);
        returnOrder.setGrandTotal(totalRefund);

        Order savedReturnOrder = orderRepository.save(returnOrder);

        // Persist return_items (linked to return order)
        for (ReturnItem ri : returnItemsToSave) {
            ri.setReturnOrder(savedReturnOrder);
        }
        returnItemRepository.saveAll(returnItemsToSave);

        // Create refund payments (same payments table, type=REFUND, amounts positive)
        List<Payment> refundPayments = new ArrayList<>();
        List<ReturnDTO.RefundResponse> refundResponses = new ArrayList<>();

        for (ReturnDTO.RefundRequest refundReq : request.getRefunds()) {
            if (refundReq.getAmount() == null || refundReq.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Refund amount must be > 0");
            }

            Payment p = new Payment();
            p.setOrder(savedReturnOrder);
            p.setMethod(refundReq.getMethod());
            p.setAmount(refundReq.getAmount().setScale(MONEY_SCALE, MONEY_ROUNDING));
            p.setType(Payment.PaymentType.REFUND);
            refundPayments.add(p);

            refundResponses.add(ReturnDTO.RefundResponse.builder()
                    .method(refundReq.getMethod())
                    .amount(p.getAmount())
                    .build());
        }
        paymentRepository.saveAll(refundPayments);

        // Update original order status based on whether all items are fully returned
        updateOriginalOrderReturnStatus(originalOrder);

        return ReturnDTO.ReturnResponse.builder()
                .returnOrderId(savedReturnOrder.getId())
                .originalOrderId(originalOrder.getId())
                .customerId(savedReturnOrder.getCustomerId())
                .totalRefund(totalRefund)
                .items(returnedItemResponses)
                .refunds(refundResponses)
                .build();
    }

    private void validateOriginalOrderEligible(Order originalOrder) {
        if (!(originalOrder.getStatus() == Order.OrderStatus.PAID || originalOrder.getStatus() == Order.OrderStatus.PARTIALLY_RETURNED)) {
            throw new BusinessRuleException("Only PAID or PARTIALLY_RETURNED orders can be returned");
        }

        if (originalOrder.getPaidAt() == null) {
            throw new BusinessRuleException("Original order must have paid_at");
        }

        LocalDateTime deadline = originalOrder.getPaidAt().plusDays(returnProperties.getWindowDays());
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessRuleException("Return window expired");
        }
    }

    private BigDecimal computeRemainingQty(Long originalOrderId, OrderItem originalItem) {
        BigDecimal alreadyReturned = returnItemRepository
                .sumReturnedQty(originalOrderId, originalItem.getId());

        BigDecimal originalQty = normalizeQty(originalItem.getQuantity());
        return originalQty.subtract(alreadyReturned).max(BigDecimal.ZERO);
    }

    private BigDecimal computeRefundAmount(OrderItem originalItem, BigDecimal returnedQty) {
        BigDecimal qty = normalizeQty(originalItem.getQuantity());
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Original item quantity invalid for item " + originalItem.getId());
        }

        BigDecimal lineTotal = originalItem.getLineTotal();
        if (lineTotal == null) {
            throw new BusinessRuleException("Original item line_total is required for item " + originalItem.getId());
        }

        // Snapshot-based: netUnit = (line_total / quantity)
        BigDecimal netUnit = lineTotal.divide(qty, 8, MONEY_ROUNDING);
        BigDecimal refundAmount = netUnit.multiply(returnedQty).setScale(MONEY_SCALE, MONEY_ROUNDING);

        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Computed refund amount must be positive");
        }
        return refundAmount;
    }

    private void updateOriginalOrderReturnStatus(Order originalOrder) {
        List<OrderItem> originalItems = orderItemRepository.findByOrderId(originalOrder.getId());

        boolean allReturned = true;
        for (OrderItem oi : originalItems) {
            BigDecimal remaining = computeRemainingQty(originalOrder.getId(), oi);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                allReturned = false;
                break;
            }
        }

        originalOrder.setStatus(allReturned ? Order.OrderStatus.RETURNED : Order.OrderStatus.PARTIALLY_RETURNED);
        orderRepository.save(originalOrder);
    }

    private BigDecimal normalizeQty(BigDecimal qty) {
        if (qty == null) {
            return BigDecimal.ZERO;
        }
        return qty.stripTrailingZeros();
    }

    private String generateReturnOrderNumber(String originalOrderNumber) {
        return "RET-" + (originalOrderNumber == null ? "ORD" : originalOrderNumber) + "-" + System.currentTimeMillis();
    }
}
