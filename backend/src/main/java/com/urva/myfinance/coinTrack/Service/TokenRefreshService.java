package com.urva.myfinance.coinTrack.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.urva.myfinance.coinTrack.Model.AngelOneAccount;
import com.urva.myfinance.coinTrack.Model.UpstoxAccount;
import com.urva.myfinance.coinTrack.Model.ZerodhaAccount;
import com.urva.myfinance.coinTrack.Repository.AngelOneAccountRepository;
import com.urva.myfinance.coinTrack.Repository.UpstoxAccountRepository;
import com.urva.myfinance.coinTrack.Repository.ZerodhaAccountRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class TokenRefreshService {

    private static final Logger logger = LoggerFactory.getLogger(TokenRefreshService.class);

    @Autowired
    private AngelOneAccountRepository angelOneAccountRepository;

    @Autowired
    private UpstoxAccountRepository upstoxAccountRepository;

    @Autowired
    private ZerodhaAccountRepository zerodhaAccountRepository;

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(3);
    private volatile boolean isRunning = false;

    /**
     * Initialize the token refresh service
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing Token Refresh Service...");

        isRunning = true;

        // Schedule token refresh tasks
        scheduleAngelOneTokenRefresh();
        scheduleUpstoxTokenRefresh();
        scheduleZerodhaTokenRefresh();

        logger.info("Token Refresh Service initialized successfully");
    }

    /**
     * Schedule Angel One token refresh (JWT expires in 8 hours)
     */
    private void scheduleAngelOneTokenRefresh() {
        // Check every 30 minutes and refresh tokens expiring within 1 hour
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshAngelOneTokens();
            } catch (Exception e) {
                logger.error("Error in Angel One token refresh task", e);
            }
        }, 5, 30, TimeUnit.MINUTES); // Start after 5 minutes, then every 30 minutes
    }

    /**
     * Schedule Upstox token refresh (Access token expires in 24 hours)
     */
    private void scheduleUpstoxTokenRefresh() {
        // Check every 2 hours and refresh tokens expiring within 4 hours
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshUpstoxTokens();
            } catch (Exception e) {
                logger.error("Error in Upstox token refresh task", e);
            }
        }, 10, 120, TimeUnit.MINUTES); // Start after 10 minutes, then every 2 hours
    }

    /**
     * Schedule Zerodha token refresh (Access token expires in 6 hours)
     */
    private void scheduleZerodhaTokenRefresh() {
        // Check every 1 hour and refresh tokens expiring within 2 hours
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshZerodhaTokens();
            } catch (Exception e) {
                logger.error("Error in Zerodha token refresh task", e);
            }
        }, 15, 60, TimeUnit.MINUTES); // Start after 15 minutes, then every 1 hour
    }

    /**
     * Refresh Angel One JWT tokens that are expiring soon
     */
    private void refreshAngelOneTokens() {
        try {
            LocalDateTime expiryThreshold = LocalDateTime.now().plusHours(1);
            List<AngelOneAccount> accounts = angelOneAccountRepository.findAll();

            int refreshedCount = 0;
            int errorCount = 0;

            for (AngelOneAccount account : accounts) {
                try {
                    // Check if token is expiring within 1 hour
                    if (account.getTokenExpiresAt() != null &&
                            account.getTokenExpiresAt().isBefore(expiryThreshold)) {

                        logger.info("Refreshing Angel One token for user: {}", account.getAppUserId());

                        // Attempt to refresh token using stored credentials
                        if (account.getAngelPin() != null && account.getAngelTotp() != null) {
                            // For Angel One, we'd need to implement a refresh method
                            // For now, log that manual re-login is needed
                            logger.warn("Angel One token expiring for user: {} - manual re-login required",
                                    account.getAppUserId());
                            errorCount++;
                        } else {
                            logger.warn("Cannot refresh Angel One token for user: {} - missing credentials",
                                    account.getAppUserId());
                            errorCount++;
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error refreshing Angel One token for user: {}",
                            account.getAppUserId(), e);
                }
            }

            if (refreshedCount > 0 || errorCount > 0) {
                logger.info("Angel One token refresh completed: {} refreshed, {} errors",
                        refreshedCount, errorCount);
            }

        } catch (Exception e) {
            logger.error("Error in Angel One token refresh process", e);
        }
    }

    /**
     * Refresh Upstox OAuth2 tokens that are expiring soon
     */
    private void refreshUpstoxTokens() {
        try {
            LocalDateTime expiryThreshold = LocalDateTime.now().plusHours(4);
            List<UpstoxAccount> accounts = upstoxAccountRepository.findAll();

            int refreshedCount = 0;
            int errorCount = 0;

            for (UpstoxAccount account : accounts) {
                try {
                    // Check if token is expiring within 4 hours
                    if (account.getTokenExpiresAt() != null &&
                            account.getTokenExpiresAt().isBefore(expiryThreshold)) {

                        logger.info("Refreshing Upstox token for user: {}", account.getAppUserId());

                        if (account.getRefreshToken() != null) {
                            // For Upstox, we'd need to implement refresh token logic
                            // For now, log that manual re-login is needed
                            logger.warn("Upstox token expiring for user: {} - refresh token logic needed",
                                    account.getAppUserId());
                            errorCount++;
                        } else {
                            logger.warn("Cannot refresh Upstox token for user: {} - missing refresh token",
                                    account.getAppUserId());
                            errorCount++;
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error refreshing Upstox token for user: {}",
                            account.getAppUserId(), e);
                }
            }

            if (refreshedCount > 0 || errorCount > 0) {
                logger.info("Upstox token refresh completed: {} refreshed, {} errors",
                        refreshedCount, errorCount);
            }

        } catch (Exception e) {
            logger.error("Error in Upstox token refresh process", e);
        }
    }

    /**
     * Refresh Zerodha tokens that are expiring soon
     */
    private void refreshZerodhaTokens() {
        try {
            LocalDateTime expiryThreshold = LocalDateTime.now().plusHours(2);
            List<ZerodhaAccount> accounts = zerodhaAccountRepository.findAll();

            int errorCount = 0;
            for (ZerodhaAccount account : accounts) {
                try {
                    // Check if access token is old (Zerodha tokens don't have explicit expiry)
                    if (account.getKiteTokenCreatedAt() != null &&
                            account.getKiteTokenCreatedAt().isBefore(expiryThreshold)) {

                        logger.info("Checking Zerodha token for user: {}", account.getAppUserId());

                        // Zerodha doesn't have refresh tokens, so we need to notify user to re-login
                        logger.warn("Zerodha token is old for user: {} - manual re-login recommended",
                                account.getAppUserId());

                        errorCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error checking Zerodha token for user: {}",
                            account.getAppUserId(), e);
                }
            }

            if (errorCount > 0) {
                logger.info("Zerodha token check completed: {} tokens need manual refresh", errorCount);
            }

        } catch (Exception e) {
            logger.error("Error in Zerodha token refresh process", e);
        }
    }

    /**
     * Force refresh all tokens (manual trigger)
     */
    public Map<String, Object> forceRefreshAllTokens() {
        logger.info("Force refreshing all broker tokens...");

        try {
            // Refresh Angel One tokens
            refreshAngelOneTokens();

            // Refresh Upstox tokens
            refreshUpstoxTokens();

            // Check Zerodha tokens
            refreshZerodhaTokens();

            return Map.of(
                    "status", "completed",
                    "message", "Token refresh process completed",
                    "timestamp", LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Error in force token refresh", e);
            return Map.of(
                    "status", "error",
                    "message", "Failed to refresh tokens: " + e.getMessage(),
                    "timestamp", LocalDateTime.now());
        }
    }

    /**
     * Get token refresh status
     */
    public Map<String, Object> getTokenRefreshStatus() {
        try {
            LocalDateTime now = LocalDateTime.now();

            // Count tokens by status
            int angelOneActive = 0, angelOneExpiring = 0, angelOneExpired = 0;
            int upstoxActive = 0, upstoxExpiring = 0, upstoxExpired = 0;
            int zerodhaActive = 0, zerodhaExpiring = 0, zerodhaExpired = 0;

            // Check Angel One accounts
            for (AngelOneAccount account : angelOneAccountRepository.findAll()) {
                if (account.getTokenExpiresAt() != null) {
                    if (account.getTokenExpiresAt().isBefore(now)) {
                        angelOneExpired++;
                    } else if (account.getTokenExpiresAt().isBefore(now.plusHours(1))) {
                        angelOneExpiring++;
                    } else {
                        angelOneActive++;
                    }
                }
            }

            // Check Upstox accounts
            for (UpstoxAccount account : upstoxAccountRepository.findAll()) {
                if (account.getTokenExpiresAt() != null) {
                    if (account.getTokenExpiresAt().isBefore(now)) {
                        upstoxExpired++;
                    } else if (account.getTokenExpiresAt().isBefore(now.plusHours(4))) {
                        upstoxExpiring++;
                    } else {
                        upstoxActive++;
                    }
                }
            }

            // Check Zerodha accounts
            for (ZerodhaAccount account : zerodhaAccountRepository.findAll()) {
                if (account.getKiteTokenCreatedAt() != null) {
                    LocalDateTime estimatedExpiry = account.getKiteTokenCreatedAt().plusHours(6);
                    if (estimatedExpiry.isBefore(now)) {
                        zerodhaExpired++;
                    } else if (estimatedExpiry.isBefore(now.plusHours(2))) {
                        zerodhaExpiring++;
                    } else {
                        zerodhaActive++;
                    }
                }
            }

            return Map.of(
                    "status", "success",
                    "isRunning", isRunning,
                    "angelOne", Map.of(
                            "active", angelOneActive,
                            "expiring", angelOneExpiring,
                            "expired", angelOneExpired),
                    "upstox", Map.of(
                            "active", upstoxActive,
                            "expiring", upstoxExpiring,
                            "expired", upstoxExpired),
                    "zerodha", Map.of(
                            "active", zerodhaActive,
                            "expiring", zerodhaExpiring,
                            "expired", zerodhaExpired),
                    "timestamp", now);

        } catch (Exception e) {
            logger.error("Error getting token refresh status", e);
            return Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", LocalDateTime.now());
        }
    }

    /**
     * Check if service is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Shutdown the token refresh service
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Token Refresh Service...");

        isRunning = false;

        try {
            scheduler.shutdown();

            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }

            logger.info("Token Refresh Service shutdown completed");

        } catch (Exception e) {
            logger.error("Error during Token Refresh Service shutdown", e);
            scheduler.shutdownNow();
        }
    }
}