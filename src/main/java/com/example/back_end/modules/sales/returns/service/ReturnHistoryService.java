package com.example.back_end.modules.sales.returns.service;

import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import com.example.back_end.modules.sales.payment.entity.Payment;
import com.example.back_end.modules.sales.payment.repository.PaymentRepository;
import com.example.back_end.modules.sales.returns.dto.ReturnDTO;
import com.example.back_end.modules.sales.returns.dto.ReturnHistoryDTO;
import com.example.back_end.modules.sales.returns.entity.ReturnItem;
import com.example.back_end.modules.sales.returns.repository.ReturnHistoryRepository;
import com.example.back_end.modules.sales.returns.repository.ReturnItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnHistoryService {

    private final ReturnHistoryRepository returnHistoryRepository;
    private final OrderRepository orderRepository;
    private final ReturnItemRepository returnItemRepository;
    private final PaymentRepository paymentRepository;

    /**
     * A) List original orders that have returns.
     */
    @Transactional(readOnly = true)
    public ReturnHistoryDTO.ReturnedOrdersPage listReturnedOrders(int limit, int offset,
                                                                 LocalDate from,
                                                                 LocalDate to,
                                                                 String q) {
        PageRequest pageable = PageRequest.of(Math.max(offset / Math.max(limit, 1), 0), limit);

        LocalDateTime fromTs = (from == null) ? null : from.atStartOfDay();
        // inclusive end-of-day
        LocalDateTime toTs = (to == null) ? null : to.atTime(LocalTime.MAX);

        Page<com.example.back_end.modules.sales.returns.repository.ReturnHistoryRepository.ReturnedOrderSummaryRow> page =
                returnHistoryRepository.findReturnedOrders(fromTs, toTs, q, pageable);

        List<ReturnHistoryDTO.ReturnedOrderSummary> items = page.getContent().stream()
                .map(r -> ReturnHistoryDTO.ReturnedOrderSummary.builder()
                        .orderId(r.getOrderId())
                        .orderNumber(r.getOrderNumber())
                        .orderDate(r.getOrderDate())
                        .customerName(r.getCustomerName())
                        .totalPaid(r.getTotalPaid())
                        .returnCount(r.getReturnCount())
                        .totalReturned(r.getTotalReturned())
                        .lastReturnAt(r.getLastReturnAt())
                        .build())
                .toList();

        return ReturnHistoryDTO.ReturnedOrdersPage.builder()
                .items(items)
                .total(page.getTotalElements())
                .limit(limit)
                .offset(offset)
                .build();
    }

    /**
     * B) List return orders for an original order.
     */
    @Transactional(readOnly = true)
    public List<ReturnHistoryDTO.ReturnOrderSummary> listReturnsForOrder(Long originalOrderId) {
        // Ensure original exists
        orderRepository.findById(originalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<Order> returnOrders = orderRepository.findReturnOrdersByOriginalOrderId(originalOrderId);

        return returnOrders.stream()
                .map(ro -> ReturnHistoryDTO.ReturnOrderSummary.builder()
                        .returnOrderId(ro.getId())
                        .returnOrderNumber(ro.getOrderNumber())
                        .createdAt(ro.getCreatedAt())
                        .totalRefund(ro.getGrandTotal() == null ? BigDecimal.ZERO : ro.getGrandTotal())
                        .itemCount(returnItemRepository.countByReturnOrderId(ro.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * C) Return details for a specific return order.
     */
    @Transactional(readOnly = true)
    public ReturnHistoryDTO.ReturnDetails getReturnDetails(Long returnOrderId) {
        Order returnOrder = orderRepository.findById(returnOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Return order not found"));

        if (returnOrder.getParentOrderId() == null || returnOrder.getStatus() != Order.OrderStatus.RETURNED) {
            throw new ResourceNotFoundException("Order is not a return order");
        }

        List<ReturnItem> items = returnItemRepository.findByReturnOrderId(returnOrderId);

        List<ReturnDTO.ReturnedItemResponse> itemResponses = items.stream()
                .map(ri -> ReturnDTO.ReturnedItemResponse.builder()
                        .originalOrderItemId(ri.getOriginalOrderItem().getId())
                        .returnedQty(ri.getReturnedQty())
                        .refundAmount(ri.getRefundAmount())
                        .build())
                .toList();

        List<Payment> payments = paymentRepository.findByOrderId(returnOrderId);
        List<ReturnDTO.RefundResponse> refundResponses = payments.stream()
                .filter(p -> p.getType() == Payment.PaymentType.REFUND)
                .map(p -> ReturnDTO.RefundResponse.builder()
                        .method(p.getMethod())
                        .amount(p.getAmount())
                        .build())
                .toList();

        return ReturnHistoryDTO.ReturnDetails.builder()
                .returnOrderId(returnOrder.getId())
                .originalOrderId(returnOrder.getParentOrderId())
                .customerId(returnOrder.getCustomerId())
                .totalRefund(returnOrder.getGrandTotal())
                .items(itemResponses)
                .refunds(refundResponses)
                .createdAt(returnOrder.getCreatedAt())
                .build();
    }
}
