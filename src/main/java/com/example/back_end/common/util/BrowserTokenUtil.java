package com.example.back_end.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Utility class for managing browser tokens in cookies
 */
public class BrowserTokenUtil {

    public static final String BROWSER_TOKEN_COOKIE_NAME = "browser_token";
    public static final String BROWSER_TOKEN_HEADER_NAME = "X-Browser-Token";
    private static final int MAX_AGE = 365 * 24 * 60 * 60; // 1 year

    /**
     * Get browser token from request (cookie or header)
     * @param request HTTP request
     * @return browser token or null
     */
    public static String getBrowserToken(HttpServletRequest request) {
        // Try cookie first
        String tokenFromCookie = getTokenFromCookie(request);
        if (tokenFromCookie != null) {
            return tokenFromCookie;
        }

        // Try header
        return request.getHeader(BROWSER_TOKEN_HEADER_NAME);
    }

    /**
     * Get browser token from cookie
     * @param request HTTP request
     * @return token or null
     */
    private static String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (BROWSER_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Set browser token in cookie
     * @param response HTTP response
     * @param token Browser token
     */
    public static void setBrowserTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(BROWSER_TOKEN_COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(MAX_AGE);
        response.addCookie(cookie);
    }

    /**
     * Generate a new browser token
     * @return New browser token
     */
    public static String generateToken() {
        return HashUtil.generateBrowserToken();
    }

    /**
     * Hash browser token for storage
     * @param token Plain browser token
     * @return Hashed token
     */
    public static String hashToken(String token) {
        return HashUtil.hashString(token);
    }

    /**
     * Clear browser token cookie
     * @param response HTTP response
     */
    public static void clearBrowserTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(BROWSER_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}