package com.example.back_end.modules.recommendation.controller;

import com.example.back_end.modules.recommendation.dto.RecommendationsResponseDTO;
import com.example.back_end.modules.recommendation.service.RecommendationsGatewayService;
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

    @GetMapping("/customers/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<RecommendationsResponseDTO> getRecommendationsForCurrentCustomer(
            @RequestParam(name = "topK", defaultValue = "10") int topK,
            @RequestParam(name = "candidateLimit", defaultValue = "500") int candidateLimit,
            @RequestParam(name = "inStockOnly", defaultValue = "true") boolean inStockOnly,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long customerId = extractCustomerId(authentication);

        int safeTopK = Math.min(Math.max(topK, 1), 100);
        int safeCandidateLimit = Math.min(Math.max(candidateLimit, 1), 2000);

        String bearerToken = resolveBearerToken(authorizationHeader, authentication);

        RecommendationsResponseDTO body = recommendationsGatewayService
                .getRecommendations(customerId, bearerToken, safeTopK, safeCandidateLimit, inStockOnly);

        return ResponseEntity.ok(body);
    }

    private Long extractCustomerId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Authentication principal is not a JWT");
        }
        Object claim = jwt.getClaims().get("userId");
        if (claim instanceof Integer) {
            return ((Integer) claim).longValue();
        }
        if (claim instanceof Long) {
            return (Long) claim;
        }
        log.error("userId claim is missing or invalid in JWT");
        throw new IllegalStateException("userId claim is missing in JWT");
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

