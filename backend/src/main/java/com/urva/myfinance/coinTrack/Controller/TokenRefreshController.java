package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.Service.TokenRefreshService;

@RestController
@RequestMapping("/api/tokens")
@CrossOrigin(origins = "*")
public class TokenRefreshController {

    private static final Logger logger = LoggerFactory.getLogger(TokenRefreshController.class);

    @Autowired
    private TokenRefreshService tokenRefreshService;

    /**
     * Get token refresh service status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getTokenStatus() {
        try {
            Map<String, Object> status = tokenRefreshService.getTokenRefreshStatus();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", status,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error getting token refresh status", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to get token status: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Force refresh all tokens
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> forceRefreshTokens() {
        try {
            Map<String, Object> result = tokenRefreshService.forceRefreshAllTokens();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", result,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error forcing token refresh", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to refresh tokens: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Check if token refresh service is running
     */
    @GetMapping("/health")
    public ResponseEntity<?> getServiceHealth() {
        try {
            boolean isRunning = tokenRefreshService.isRunning();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "isRunning", isRunning,
                            "service", "TokenRefreshService",
                            "uptime", isRunning ? "Active" : "Inactive"),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error checking token refresh service health", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to check service health: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }
}