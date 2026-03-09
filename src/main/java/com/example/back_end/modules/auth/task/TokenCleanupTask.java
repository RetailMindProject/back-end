package com.example.back_end.modules.auth.task;

import com.example.back_end.modules.auth.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for token maintenance.
 *
 * <p>This component runs periodic cleanup jobs to:
 * <ul>
 *   <li>Remove expired verification tokens from the database</li>
 *   <li>Keep the database clean and performant</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {

    private final VerificationTokenService tokenService;

    /**
     * Clean up expired tokens.
     * Runs daily at 2:00 AM server time.
     *
     * <p>Cron expression: "0 0 2 * * ?" means:
     * <ul>
     *   <li>Second: 0</li>
     *   <li>Minute: 0</li>
     *   <li>Hour: 2 (2 AM)</li>
     *   <li>Day of month: * (every day)</li>
     *   <li>Month: * (every month)</li>
     *   <li>Day of week: ? (any)</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired tokens...");

        try {
            int deletedCount = tokenService.cleanupExpiredTokens();

            if (deletedCount > 0) {
                log.info("Scheduled cleanup completed: {} expired tokens removed", deletedCount);
            } else {
                log.debug("Scheduled cleanup completed: no expired tokens found");
            }
        } catch (Exception e) {
            log.error("Error during scheduled token cleanup", e);
        }
    }

    /**
     * Alternative: Clean up more frequently (every 6 hours).
     * Uncomment if you want more aggressive cleanup.
     */
    // @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // Every 6 hours
    // public void cleanupExpiredTokensFrequent() {
    //     cleanupExpiredTokens();
    // }
}

