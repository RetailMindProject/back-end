package com.example.back_end.modules.recommendation.controller;

import com.example.back_end.modules.recommendation.dto.RecommendationsResponseDTO;
import com.example.back_end.modules.recommendation.service.RecommendationsGatewayService;
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

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationsGatewayController {

    private final RecommendationsGatewayService recommendationsGatewayService;
    private final JwtService jwtService;

    @GetMapping("/customers/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RecommendationsResponseDTO> getRecommendationsForCurrentCustomer(
            @RequestParam(name = "topK", defaultValue = "10") int topK,
            @RequestParam(name = "candidateLimit", defaultValue = "500") int candidateLimit,
            @RequestParam(name = "inStockOnly", defaultValue = "true") boolean inStockOnly,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long customerId = extractCustomerId(authentication, authorizationHeader);

        int safeTopK = Math.min(Math.max(topK, 1), 100);
        int safeCandidateLimit = Math.min(Math.max(candidateLimit, 1), 2000);

        String bearerToken = resolveBearerToken(authorizationHeader, authentication);

        RecommendationsResponseDTO body = recommendationsGatewayService
                .getRecommendations(customerId, bearerToken, safeTopK, safeCandidateLimit, inStockOnly);

        return ResponseEntity.ok(body);
    }

    /**
     * Extract customerId/userId from authentication.
     * Supports both:
     * 1. Jwt principal (future OAuth2 setup)
     * 2. UsernamePasswordAuthenticationToken with email principal (current custom filter setup)
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

    private String resolveBearerToken(String authorizationHeader, Authentication authentication) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader;
        }
        if (authentication != null && authentication.getCredentials() instanceof String token) {
            return "Bearer " + token;
        }
        log.warn("Authorization header is missing; forwarding call without token may fail in downstream service");
        return "";
    }
}

