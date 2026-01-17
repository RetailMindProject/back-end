package com.example.back_end.modules.auth.scheduler;

import com.example.back_end.modules.auth.service.PendingRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for authentication-related cleanup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthScheduledTasks {

    private final PendingRegistrationService pendingRegistrationService;

    /**
     * Clean up expired pending registrations.
     * Runs every 6 hours.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void cleanupExpiredPendingRegistrations() {
        log.info("Starting scheduled cleanup of expired pending registrations");
        int deletedCount = pendingRegistrationService.cleanupExpiredPendingRegistrations();
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired pending registrations", deletedCount);
        } else {
            log.debug("No expired pending registrations to clean up");
        }
    }
}

