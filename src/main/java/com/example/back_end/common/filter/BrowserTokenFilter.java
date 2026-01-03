package com.example.back_end.common.filter;

import com.example.back_end.common.dto.BrowserContext;
import com.example.back_end.common.util.BrowserTokenUtil;
import com.example.back_end.modules.terminal.entity.TerminalDevice;
import com.example.back_end.modules.terminal.repository.TerminalDeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Filter to handle browser token in every request
 * - Reads browser_token from cookie/header
 * - If not exists, generates new token and sets cookie
 * - Checks if browser is paired with a terminal
 * - Sets BrowserContext as request attribute
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BrowserTokenFilter extends OncePerRequestFilter {

    private final TerminalDeviceRepository terminalDeviceRepository;

    public static final String BROWSER_CONTEXT_ATTRIBUTE = "browserContext";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Get or generate browser token
        String browserToken = BrowserTokenUtil.getBrowserToken(request);
        boolean isNewToken = false;

        if (browserToken == null || browserToken.isBlank()) {
            // Generate new token
            browserToken = BrowserTokenUtil.generateToken();
            BrowserTokenUtil.setBrowserTokenCookie(response, browserToken);
            isNewToken = true;
            log.debug("Generated new browser token");
        }

        // Hash the token for DB queries
        String browserTokenHash = BrowserTokenUtil.hashToken(browserToken);

        // ✅ Check if browser is paired with a terminal (active only)
        Optional<TerminalDevice> deviceOpt = terminalDeviceRepository
                .findByTokenHashAndRevokedAtIsNull(browserTokenHash);

        BrowserContext context = BrowserContext.builder()
                .browserToken(browserToken)
                .browserTokenHash(browserTokenHash)
                .isNewToken(isNewToken)
                .build();

        if (deviceOpt.isPresent()) {
            TerminalDevice device = deviceOpt.get();
            context.setPaired(true);
            context.setTerminalId(device.getTerminalId());
            context.setDeviceId(device.getId());

            // ✅ Update last seen with error handling
            try {
                device.setLastSeenAt(LocalDateTime.now());
                terminalDeviceRepository.saveAndFlush(device);
            } catch (Exception e) {
                // Not critical - log and continue
                log.debug("Failed to update terminal device last_seen_at: {}", e.getMessage());
            }
        } else {
            context.setPaired(false);
        }

        // Set context as request attribute
        request.setAttribute(BROWSER_CONTEXT_ATTRIBUTE, context);

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Get BrowserContext from request
     * @param request HTTP request
     * @return BrowserContext
     */
    public static BrowserContext getContext(HttpServletRequest request) {
        return (BrowserContext) request.getAttribute(BROWSER_CONTEXT_ATTRIBUTE);
    }
}