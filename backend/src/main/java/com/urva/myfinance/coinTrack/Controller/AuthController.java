package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.DTO.AuthRequest;
import com.urva.myfinance.coinTrack.DTO.AuthResponse;
import com.urva.myfinance.coinTrack.Model.User;
import com.urva.myfinance.coinTrack.Service.AuthService;
import com.urva.myfinance.coinTrack.Service.UserService;

@RestController
@CrossOrigin
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    @Autowired
    private AuthService authService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ===== LOGIN FUNCTIONALITY =====

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            // Convert AuthRequest to User for service method
            User user = new User();
            user.setUsername(authRequest.getUsernameOrEmail()); // This will be handled in service
            user.setPassword(authRequest.getPassword());

            AuthResponse result = userService.verifyUser(user);
            return ResponseEntity.ok(result);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Login error: " + e.getMessage());
        }
    }

    // ===== TOKEN REFRESH FUNCTIONALITY =====

    /**
     * Get token refresh service status
     */
    @GetMapping("/api/tokens/status")
    public ResponseEntity<?> getTokenStatus() {
        try {
            Map<String, Object> status = authService.getTokenRefreshStatus();

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
    @PostMapping("/api/tokens/refresh")
    public ResponseEntity<?> forceRefreshTokens() {
        try {
            Map<String, Object> result = authService.forceRefreshAllTokens();

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
    @GetMapping("/api/tokens/health")
    public ResponseEntity<?> getServiceHealth() {
        try {
            boolean isRunning = authService.isRunning();

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
