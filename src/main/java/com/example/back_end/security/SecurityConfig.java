package com.example.back_end.security;

import com.example.back_end.common.filter.BrowserTokenFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final BrowserTokenFilter browserTokenFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Add BrowserTokenFilter BEFORE authentication
                .addFilterBefore(browserTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - NO authentication
                        .requestMatchers("/api/auth/login", "/api/auth/test").permitAll()
                        .requestMatchers("/api/auth/register/ceo").permitAll()

                        // Terminal pairing endpoints (public)
                        .requestMatchers(
                                "/api/terminal/pair",
                                "/api/terminal/switch",
                                "/api/terminal/unpair",
                                "/api/terminal/pairing-status",
                                "/api/terminal/current",
                                "/api/terminal/available"
                        ).permitAll()

                        // ✅ NEW - Pairing requests (cashier side - public with JWT)
                        .requestMatchers(HttpMethod.POST, "/api/pairing-requests").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pairing-requests/status").permitAll()

                        // ✅ NEW - Pairing requests (manager side - requires role)
                        .requestMatchers(HttpMethod.GET, "/api/pairing-requests/pending").hasAnyRole("STORE_MANAGER", "CEO")
                        .requestMatchers(HttpMethod.POST, "/api/pairing-requests/*/approve").hasAnyRole("STORE_MANAGER", "CEO")
                        .requestMatchers(HttpMethod.POST, "/api/pairing-requests/*/reject").hasAnyRole("STORE_MANAGER", "CEO")

                        // Terminal pairing code generation (CEO only)
                        .requestMatchers("/api/terminal/pairing-code").hasRole("CEO")

                        // Cashier endpoints (public - browser token is enough)
                        .requestMatchers("/api/sessions/cashier/**").permitAll()

                        // Session Management endpoints - Order matters! More specific first
                        .requestMatchers("/api/sessions/*/close").hasRole("CEO")
                        .requestMatchers("/api/sessions/active").hasAnyRole("CEO", "STORE_MANAGER")
                        .requestMatchers("/api/sessions").hasAnyRole("CEO", "STORE_MANAGER")
                        .requestMatchers("/api/sessions/*").permitAll()

                        // Terminal management (STORE_MANAGER and CEO)
                        .requestMatchers("/api/terminal/management/**").hasAnyRole("STORE_MANAGER", "CEO")

                        // Other terminal endpoints (authenticated)
                        .requestMatchers("/api/terminal/**").authenticated()

                        // Public endpoints
                        .requestMatchers("/api/orders/**").permitAll()
                        .requestMatchers("/api/returns/**").permitAll()
                        .requestMatchers("/api/categories/**").permitAll()
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/forecasting/**").permitAll()

                        // Role-based endpoints
                        .requestMatchers("/api/dashboard/store/**").hasAnyRole("STORE_MANAGER", "CEO")
                        .requestMatchers("/api/dashboard/inventory/**").hasAnyRole("INVENTORY_MANAGER", "CEO")

                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:5176",
                "http://localhost:5174",
                "http://localhost:3000"
        ));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "X-Browser-Token", "Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}