package com.example.back_end.modules.customer.controller;

import com.example.back_end.modules.customer.dto.CustomerOrdersResponseDTO;
import com.example.back_end.modules.sales.order.service.OrderService;
import com.example.back_end.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Controller for customer-facing order operations.
 * Customers can view their own order history.
 */
@RestController
@RequestMapping("/api/customers/me")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CustomerOrderController {

    private final OrderService orderService;
    private final JwtService jwtService;

    /**
     * Get authenticated customer's order history.
     *
     * GET /api/customers/me/orders?limit=50&since=2025-01-01T00:00:00.000Z
     *
     * @param limit Maximum number of orders to return (1-100, default 50)
     * @param since Optional ISO 8601 datetime to filter orders created after this time
     * @param authorizationHeader Authorization header containing JWT token
     * @param authentication Spring Security Authentication object
     * @return List of customer's orders with items
     */
    @GetMapping("/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyOrders(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String since,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            Authentication authentication
    ) {
        try {
            // Extract customerId (userId) from JWT
            Long userId = extractCustomerId(authentication, authorizationHeader);
            log.info("Fetching orders for customer userId: {}", userId);

            // Validate and clamp limit to safe range
            int safeLimit = Math.min(Math.max(limit, 1), 100);

            // Parse since date if provided
            LocalDateTime sinceDate = null;
            if (StringUtils.hasText(since)) {
                try {
                    sinceDate = LocalDateTime.parse(since, DateTimeFormatter.ISO_DATE_TIME);
                    log.debug("Filtering orders since: {}", sinceDate);
                } catch (DateTimeParseException e) {
                    log.warn("Invalid since date format: {}", since);
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "status", 400,
                                    "message", "Invalid date format for 'since' parameter. Use ISO 8601 format (e.g., 2025-01-01T00:00:00.000Z)",
                                    "timestamp", LocalDateTime.now().toString()
                            ));
                }
            }

            // Fetch orders for this customer
            CustomerOrdersResponseDTO orders = orderService.getCustomerOrders(userId, safeLimit, sinceDate);

            log.info("Returning {} orders for customer userId: {}", orders.getOrders().size(), userId);
            return ResponseEntity.ok(orders);

        } catch (IllegalStateException e) {
            log.error("Customer ID extraction failed: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "status", 401,
                            "message", "Unauthorized - " + e.getMessage(),
                            "timestamp", LocalDateTime.now().toString()
                    ));
        } catch (Exception e) {
            log.error("Error fetching customer orders", e);
            return ResponseEntity.status(500)
                    .body(Map.of(
                            "status", 500,
                            "message", "Internal server error: " + e.getMessage(),
                            "timestamp", LocalDateTime.now().toString()
                    ));
        }
    }

    /**
     * Extract customerId/userId from authentication.
     * Supports both Jwt principal and UsernamePasswordAuthenticationToken.
     * Same logic as RecommendationsGatewayController.
     *
     * @param authentication Spring Security Authentication object
     * @param authorizationHeader Authorization header value (e.g., "Bearer <token>")
     * @return Numeric userId extracted from JWT claims
     * @throws IllegalStateException if authentication is null or userId claim is missing
     */
    private Long extractCustomerId(Authentication authentication, String authorizationHeader) {
        if (authentication == null) {
            throw new IllegalStateException("Authentication is null");
        }

        // Case 1: Jwt object as principal (OAuth2 resource server setup)
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return extractUserIdFromJwt(jwt);
        }

        // Case 2: UsernamePasswordAuthenticationToken (current custom filter setup)
        // Principal is email (String), so we need to extract userId from the JWT token itself
        String email = (String) authentication.getPrincipal();

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            try {
                String jwtToken = authorizationHeader.substring(7);
                Long userId = jwtService.extractUserId(jwtToken);
                if (userId != null) {
                    return userId;
                }
                log.error("userId claim is missing in JWT for email: {}", email);
                throw new IllegalStateException("userId claim is missing in JWT for email: " + email);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to extract userId from JWT token for email {}: {}", email, e.getMessage(), e);
                throw new IllegalStateException("Failed to extract userId from JWT token for email: " + email, e);
            }
        }

        log.error("Authorization header missing or invalid; cannot extract userId for email: {}", email);
        throw new IllegalStateException("Authorization header missing or invalid; cannot extract userId");
    }

    /**
     * Extract userId from Jwt claims object.
     * Supports userId claim type as Integer, Long, or String.
     *
     * @param jwt Spring Security Jwt object
     * @return Numeric userId
     * @throws IllegalStateException if userId claim is missing or invalid
     */
    private Long extractUserIdFromJwt(Jwt jwt) {
        Object claim = jwt.getClaims().get("userId");
        Long userId = toUserIdLong(claim);
        if (userId != null) {
            return userId;
        }
        log.error("userId claim is missing or invalid in Jwt principal");
        throw new IllegalStateException("userId claim is missing or invalid in Jwt principal");
    }

    /**
     * Convert userId claim value to Long.
     * Supports Integer, Long, and String types.
     *
     * @param value The claim value from JWT
     * @return Long value or null if claim is null
     */
    private Long toUserIdLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("userId claim is a String but not a valid long: {}", value);
                return null;
            }
        }
        log.warn("userId claim has unsupported type: {}", value.getClass().getCanonicalName());
        return null;
    }
}

