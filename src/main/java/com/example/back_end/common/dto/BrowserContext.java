package com.example.back_end.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Context object to hold browser token information
 * This is typically set by a filter and used throughout the request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserContext {

    /**
     * Plain browser token (from cookie/header)
     */
    private String browserToken;

    /**
     * Hashed version of browser token (for DB queries)
     */
    private String browserTokenHash;

    /**
     * Whether this is a new token (just generated)
     */
    private boolean isNewToken;

    /**
     * Terminal ID if paired (null if not paired)
     */
    private Long terminalId;

    /**
     * Whether browser is paired with a terminal
     */
    private boolean isPaired;

    /**
     * Terminal device ID if paired
     */
    private Long deviceId;
}