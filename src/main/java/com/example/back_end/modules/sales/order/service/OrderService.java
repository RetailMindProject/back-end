package com.example.back_end.modules.sales.order.service;

import com.example.back_end.exception.BusinessRuleException;
import com.example.back_end.exception.ResourceNotFoundException;
import com.example.back_end.modules.cashier.entity.Session;
import com.example.back_end.modules.cashier.repository.SessionRepository;
import com.example.back_end.modules.catalog.product.entity.Product;
import com.example.back_end.modules.catalog.product.repository.ProductRepository;
import com.example.back_end.modules.offer.dto.OfferApplicationResult;
import com.example.back_end.modules.offer.entity.Offer;
import com.example.back_end.modules.offer.service.BundleOfferService;
import com.example.back_end.modules.offer.dto.BundleApplicationResult;
import com.example.back_end.modules.offer.service.CategoryOfferService;
import com.example.back_end.modules.offer.service.OfferEngine;
import com.example.back_end.modules.offer.service.ProductOfferService;
import com.example.back_end.modules.sales.order.dto.OrderDTO;
import com.example.back_end.modules.sales.order.entity.Order;
import com.example.back_end.modules.sales.order.entity.OrderItem;
import com.example.back_end.modules.sales.order.mapper.OrderMapper;
import com.example.back_end.modules.sales.order.repository.OrderItemRepository;
import com.example.back_end.modules.sales.order.repository.OrderRepository;
import com.example.back_end.modules.sales.payment.entity.Payment;
import com.example.back_end.modules.sales.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for POS order operations
 * Handles all offer types with proper priority:
 * 1. BUNDLE offers (highest priority - locks items)
 * 2. PRODUCT offers (item level)
 * 3. CATEGORY offers (item level)
 * 4. ORDER offers (order level)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final SessionRepository sessionRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final ProductOfferService productOfferService;
    private final CategoryOfferService categoryOfferService;
    private final BundleOfferService bundleOfferService;
    private final OfferEngine offerEngine;

    /**
     * Create new order
     */
    @Transactional
    public OrderDTO.OrderResponse createOrder(OrderDTO.CreateRequest request) {
        // Validate session - use the session passed from controller (already validated)
        Session session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Double-check session is open (defensive check) - Session uses String, not enum
        if (!Session.SessionStatus.OPEN.equals(session.getStatus())) {
            log.warn("Attempted to create order on closed session {}. Status: {}", 
                    session.getId(), session.getStatus());
            throw new BusinessRuleException("Cannot create order on closed session");
        }

        // Create order
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setSession(session);
        order.setStatus(Order.OrderStatus.DRAFT);
        order.setSubtotal(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setGrandTotal(BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toOrderResponse(savedOrder, List.of(), List.of());
    }

    /**
     * Add item to order (or update quantity if exists)
     */
    @Transactional
    public OrderDTO.OrderResponse addItem(OrderDTO.AddItemRequest request) {
        // Get order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new BusinessRuleException("Cannot modify paid order");
        }

        // Validate product exists and get price
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.getIsActive()) {
            throw new BusinessRuleException("Product is not active");
        }

        // Check if item already exists
        Optional<OrderItem> existingItem = orderItemRepository
                .findByOrderIdAndProductId(request.getOrderId(), request.getProductId());

        OrderItem item;
        if (existingItem.isPresent()) {
            // Update quantity (add to existing)
            item = existingItem.get();
            item.setQuantity(item.getQuantity().add(request.getQuantity()));
        } else {
            // Create new item
            item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setUnitPrice(product.getDefaultPrice());
            item.setQuantity(request.getQuantity());
        }

        // Initialize with no discount
        item.setLineDiscount(BigDecimal.ZERO);
        item.setOfferId(null);

        // Calculate initial line total (will be recalculated after offers)
        BigDecimal lineTotal = item.getUnitPrice().multiply(item.getQuantity());
        item.setLineTotal(lineTotal);

        orderItemRepository.save(item);

        // Apply offers to ALL items (checks bundles first!)
        applyOffersToItems(order);

        // Recalculate totals for all items
        recalculateItemTotals(order);

        // Recalculate order totals
        recalculateOrderTotals(order);

        return getOrderById(order.getId());
    }

    /**
     * Update item quantity (increment based on orderId + productId)
     *
     * request.quantity هنا هي مقدار الزيادة (delta) وليس الكمية النهائية.
     * مثال: الكمية الحالية 5، والـ quantity في الطلب = 2 → الكمية الجديدة = 7
     */
    @Transactional
    public OrderDTO.OrderResponse updateItemQuantity(OrderDTO.UpdateItemRequest request) {
        if (request.getQuantity() == null) {
            throw new BusinessRuleException("Quantity is required");
        }

        BigDecimal delta = request.getQuantity();

        // لا معنى لـ 0 كـ delta
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("Quantity change cannot be zero");
        }

        // نجيب الـ item باستخدام (orderId + productId)
        OrderItem item = orderItemRepository
                .findByOrderIdAndProductId(request.getOrderId(), request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found for this order and product"));

        Order order = item.getOrder();
        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new BusinessRuleException("Cannot modify paid order");
        }

        BigDecimal currentQty = item.getQuantity();
        BigDecimal newQuantity = currentQty.add(delta);

        if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            // لو النتيجة صفر أو أقل → نحذف الـ item من الطلب
            orderItemRepository.delete(item);
        } else {
            item.setQuantity(newQuantity);
            orderItemRepository.save(item);
        }

        // Re-apply offers (bundle might change!)
        applyOffersToItems(order);

        // Recalculate totals
        recalculateItemTotals(order);
        recalculateOrderTotals(order);

        return getOrderById(order.getId());
    }

    /**
     * Remove item from order
     */
    @Transactional
    public OrderDTO.OrderResponse removeItem(Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        Order order = item.getOrder();
        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new BusinessRuleException("Cannot modify paid order");
        }

        orderItemRepository.delete(item);

        // Re-apply offers (bundle might change!)
        applyOffersToItems(order);

        // Recalculate order totals
        recalculateItemTotals(order);
        recalculateOrderTotals(order);

        return getOrderById(order.getId());
    }

    /**
     * Apply discount to order
     */
    @Transactional
    public OrderDTO.OrderResponse applyDiscount(OrderDTO.ApplyDiscountRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new BusinessRuleException("Cannot modify paid order");
        }

        // Calculate manual discount
        BigDecimal discountAmount;
        if (request.getDiscountAmount() != null && request.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = request.getDiscountAmount();
        } else if (request.getDiscountPercentage() != null && request.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = order.getSubtotal()
                    .multiply(request.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discountAmount = BigDecimal.ZERO;
        }

        // Set manual discount directly (bypasses ORDER offer)
        order.setDiscountAmount(discountAmount);

        // Recalculate tax and grand total manually (don't call recalculateOrderTotals)
        BigDecimal taxRate = BigDecimal.valueOf(0.10);
        BigDecimal taxableAmount = order.getSubtotal().subtract(discountAmount);
        BigDecimal taxAmount = taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        order.setTaxAmount(taxAmount);

        BigDecimal grandTotal = order.getSubtotal()
                .subtract(discountAmount)
                .add(taxAmount);
        order.setGrandTotal(grandTotal);

        orderRepository.save(order);

        return getOrderById(order.getId());
    }

    /**
     * Process payment for order
     */
    @Transactional
    public OrderDTO.OrderResponse processPayment(OrderDTO.PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new BusinessRuleException("Order already paid");
        }

        // Handle payment based on method
        String paymentMethodStr = request.getPaymentMethod().toUpperCase();

        if ("SPLIT".equals(paymentMethodStr)) {
            // Split payment: cash + card - create two separate payments
            if (request.getCashAmount() == null || request.getCardAmount() == null) {
                throw new BusinessRuleException("Split payment requires both cash and card amounts");
            }

            BigDecimal total = request.getCashAmount().add(request.getCardAmount());
            if (total.compareTo(order.getGrandTotal()) != 0) {
                throw new BusinessRuleException("Total payment amount (" + total + ") must equal order total (" + order.getGrandTotal() + ")");
            }

            // Create cash payment
            Payment cashPayment = createPayment(order, Payment.PaymentMethod.CASH, request.getCashAmount());
            paymentRepository.save(cashPayment);

            // Create card payment
            Payment cardPayment = createPayment(order, Payment.PaymentMethod.CARD, request.getCardAmount());
            paymentRepository.save(cardPayment);

        } else {
            // Single payment method (CASH or CARD)
            if (request.getAmount() == null) {
                throw new BusinessRuleException("Amount is required for " + paymentMethodStr + " payment");
            }

            Payment.PaymentMethod paymentMethod = Payment.PaymentMethod.valueOf(paymentMethodStr);

            // Validate amount matches order total
            if (request.getAmount().compareTo(order.getGrandTotal()) != 0) {
                throw new BusinessRuleException("Payment amount (" + request.getAmount() + ") must equal order total (" + order.getGrandTotal() + ")");
            }

            Payment payment = createPayment(order, paymentMethod, request.getAmount());
            paymentRepository.save(payment);
        }

        // Mark order as paid
        order.setStatus(Order.OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        return getOrderById(order.getId());
    }

    /**
     * Get order by ID
     */
    @Transactional(readOnly = true)
    public OrderDTO.OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<Payment> payments = paymentRepository.findByOrderId(orderId);

        return orderMapper.toOrderResponse(order, items, payments);
    }

    /**
     * Get order history for session
     */
    @Transactional(readOnly = true)
    public List<OrderDTO.OrderSummary> getSessionOrderHistory(Long sessionId) {
        List<Order> orders = orderRepository.findBySessionId(sessionId);
        return orders.stream()
                .map(orderMapper::toOrderSummary)
                .toList();
    }

    /**
     * Get draft/held orders for session
     */
    @Transactional(readOnly = true)
    public List<OrderDTO.OrderSummary> getDraftOrders(Long sessionId) {
        List<Order> orders = orderRepository.findDraftOrdersBySession(sessionId);
        return orders.stream()
                .map(orderMapper::toOrderSummary)
                .toList();
    }

    /**
     * Hold order (save for later)
     */
    @Transactional
    public OrderDTO.OrderResponse holdOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new BusinessRuleException("Cannot hold paid order");
        }

        order.setStatus(Order.OrderStatus.HOLD);
        orderRepository.save(order);

        return getOrderById(orderId);
    }

    /**
     * Retrieve held order
     */
    @Transactional
    public OrderDTO.OrderResponse retrieveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != Order.OrderStatus.HOLD) {
            throw new BusinessRuleException("Only held orders can be retrieved");
        }

        order.setStatus(Order.OrderStatus.DRAFT);
        orderRepository.save(order);

        return getOrderById(orderId);
    }

    /**
     * Delete/void order
     */
    @Transactional
    public void voidOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == Order.OrderStatus.PAID) {
            throw new BusinessRuleException("Cannot void paid order. Use refund instead.");
        }

        orderRepository.delete(order);
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Generate unique order number
     */
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * Create payment entity
     */
    private Payment createPayment(Order order, Payment.PaymentMethod method, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setAmount(amount);
        return payment;
    }

    /**
     * Apply offers to ALL order items
     *
     * Priority:
     * 1. BUNDLE offer (highest) - locks items from other offers
     * 2. PRODUCT offer
     * 3. CATEGORY offer
     *
     * @param order The order containing items
     */
    private void applyOffersToItems(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        if (items.isEmpty()) {
            return;
        }

        // Step 1: Check for BUNDLE offers (highest priority)
        BundleApplicationResult bundleResult = // ✅ صح
                bundleOfferService.detectAndApplyBundles(items);

        Set<Long> itemsInBundle = new HashSet<>();

        if (bundleResult.getBundleApplied()) {
            // Apply bundle discounts to items
            Map<Long, BigDecimal> bundleDiscounts = bundleResult.getItemDiscounts();

            for (OrderItem item : items) {
                if (bundleDiscounts.containsKey(item.getId())) {
                    // This item is part of bundle
                    BigDecimal discount = bundleDiscounts.get(item.getId());
                    item.setOfferId(bundleResult.getOfferId());
                    item.setLineDiscount(discount);
                    itemsInBundle.add(item.getId());

                    log.info("Applied BUNDLE discount to item {}: ${}", item.getId(), discount);
                }
            }
        }

        // Step 2: For items NOT in bundle, apply Product/Category offers
        for (OrderItem item : items) {
            if (!itemsInBundle.contains(item.getId())) {
                // Not in bundle - apply regular item offers
                applyRegularItemOffer(item);
            }
        }

        // Save all items
        orderItemRepository.saveAll(items);
    }

    /**
     * Apply regular item offer (Product or Category)
     * Only called for items NOT in a bundle
     */
    private void applyRegularItemOffer(OrderItem item) {
        if (item.getProduct() == null || item.getProduct().getId() == null) {
            return;
        }

        Offer bestOffer = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        String offerType = null;

        // Priority 1: PRODUCT offer
        Optional<Offer> productOffer = productOfferService.findBestProductOffer(
                item.getProduct().getId(),
                item.getUnitPrice(),
                item.getQuantity()
        );

        if (productOffer.isPresent()) {
            BigDecimal discount = productOfferService.calculateProductOfferDiscount(
                    productOffer.get(),
                    item.getUnitPrice(),
                    item.getQuantity()
            );

            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                bestOffer = productOffer.get();
                bestDiscount = discount;
                offerType = "PRODUCT";
            }
        }

        // Priority 2: CATEGORY offer (if no product offer)
        if (bestOffer == null) {
            Optional<Offer> categoryOffer = categoryOfferService.findBestCategoryOffer(
                    item.getProduct().getId(),
                    item.getUnitPrice(),
                    item.getQuantity()
            );

            if (categoryOffer.isPresent()) {
                BigDecimal discount = categoryOfferService.calculateCategoryOfferDiscount(
                        categoryOffer.get(),
                        item.getUnitPrice(),
                        item.getQuantity()
                );

                if (discount.compareTo(BigDecimal.ZERO) > 0) {
                    bestOffer = categoryOffer.get();
                    bestDiscount = discount;
                    offerType = "CATEGORY";
                }
            }
        }

        // Apply best offer found
        if (bestOffer != null) {
            item.setOfferId(bestOffer.getId());
            item.setLineDiscount(bestDiscount);

            log.info("Applied {} offer to item {}: offer '{}' (ID={}), discount=${}",
                    offerType, item.getId(), bestOffer.getTitle(), bestOffer.getId(), bestDiscount);
        } else {
            item.setOfferId(null);
            item.setLineDiscount(BigDecimal.ZERO);
        }
    }

    /**
     * Recalculate line totals for all items after discounts applied
     */
    private void recalculateItemTotals(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        for (OrderItem item : items) {
            BigDecimal lineDiscount = item.getLineDiscount() != null ?
                    item.getLineDiscount() : BigDecimal.ZERO;
            BigDecimal taxAmount = item.getTaxAmount() != null ?
                    item.getTaxAmount() : BigDecimal.ZERO;

            BigDecimal lineTotal = item.getUnitPrice()
                    .multiply(item.getQuantity())
                    .subtract(lineDiscount)
                    .add(taxAmount);

            item.setLineTotal(lineTotal);
        }

        orderItemRepository.saveAll(items);
    }

    /**
     * Recalculate order totals
     *
     * Flow:
     * 1. Calculate subtotal from order items (after product/category/bundle discounts)
     * 2. Apply ORDER-level offer automatically (threshold-based)
     * 3. Calculate tax on (subtotal - order_discount)
     * 4. Calculate grand total
     */
    private void recalculateOrderTotals(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        // Step 1: Calculate subtotal from items (includes item-level discounts)
        BigDecimal subtotal = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setSubtotal(subtotal);

        // Step 2: Apply ORDER-level offer automatically (threshold-based)
        try {
            OfferApplicationResult orderOfferResult = offerEngine.applyOrderOffer(order, subtotal);
            BigDecimal orderDiscount = orderOfferResult.getDiscountAmount();

            log.debug("ORDER offer applied: {}, discount: ${}",
                    orderOfferResult.getOfferApplied(), orderDiscount);

        } catch (Exception e) {
            log.error("Error applying ORDER offer: ", e);
            order.setDiscountAmount(BigDecimal.ZERO);
        }

        BigDecimal orderDiscount = order.getDiscountAmount() != null ?
                order.getDiscountAmount() : BigDecimal.ZERO;

        // Step 3: Calculate tax on (subtotal - order_discount)
        BigDecimal taxRate = BigDecimal.valueOf(0.10);
        BigDecimal taxableAmount = subtotal.subtract(orderDiscount);
        BigDecimal taxAmount = taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        order.setTaxAmount(taxAmount);

        // Step 4: Calculate grand total
        BigDecimal grandTotal = subtotal
                .subtract(orderDiscount)
                .add(taxAmount);
        order.setGrandTotal(grandTotal);

        orderRepository.save(order);
    }
}