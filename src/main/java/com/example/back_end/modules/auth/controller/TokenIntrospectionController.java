package com.example.back_end.modules.auth.controller;

import com.example.back_end.modules.auth.dto.TokenIntrospectionResponseDTO;
import com.example.back_end.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/public/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TokenIntrospectionController {

    private final JwtService jwtService;

    /**
     * Token introspection endpoint for external services (RAG).
     *
     * Validates JWT token without exposing signing secret.
     * Used by RAG service to confirm customer identity before processing requests.
     *
     * Authorization header is required with valid customer token.
     * Only CUSTOMER role tokens are accepted.
     *
     * @param authHeader Authorization header (Bearer <JWT>)
     * @return Token validation response with user info (if valid)
     */
    @GetMapping("/introspect")
    public ResponseEntity<TokenIntrospectionResponseDTO> introspectToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Extract token from header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(TokenIntrospectionResponseDTO.builder()
                    .valid(false)
                    .build());
        }

        String token = authHeader.substring(7);

        try {
            // Extract claims from token
            Claims claims = jwtService.extractAllClaims(token);

            // Validate token is not expired
            if (claims.getExpiration().before(new Date())) {
                return ResponseEntity.ok(TokenIntrospectionResponseDTO.builder()
                        .valid(false)
                        .build());
            }

            // Extract user info from claims
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            Integer userId = claims.get("userId", Integer.class);
            String firstName = claims.get("firstName", String.class);
            String lastName = claims.get("lastName", String.class);
            Long expiresAt = claims.getExpiration().getTime();

            // Only accept CUSTOMER role for introspection
            if (!"CUSTOMER".equals(role)) {
                return ResponseEntity.ok(TokenIntrospectionResponseDTO.builder()
                        .valid(false)
                        .build());
            }

            // Return valid token info
            return ResponseEntity.ok(TokenIntrospectionResponseDTO.builder()
                    .valid(true)
                    .userId(userId)
                    .email(email)
                    .role(role)
                    .firstName(firstName)
                    .lastName(lastName)
                    .expiresAt(expiresAt)
                    .build());

        } catch (Exception e) {
            // Token is invalid or malformed
            return ResponseEntity.ok(TokenIntrospectionResponseDTO.builder()
                    .valid(false)
                    .build());
        }
    }
}

