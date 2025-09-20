package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    private final UserService userService;

    @Autowired
    private AuthService authService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // ===== LOGIN FUNCTIONALITY =====

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        // Convert AuthRequest to User for service method
        User user = new User();
        user.setUsername(authRequest.getUsernameOrEmail()); // This will be handled in service
        user.setPassword(authRequest.getPassword());

        AuthResponse result = userService.verifyUser(user);
        return ResponseEntity.ok(result);
    }

    // ===== TOKEN REFRESH FUNCTIONALITY =====

    /**
     * Get token refresh service status
     */
    @GetMapping("/api/tokens/status")
    public ResponseEntity<?> getTokenStatus() {
        Map<String, Object> status = authService.getTokenRefreshStatus();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", status,
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    /**
     * Force refresh all tokens
     */
    @PostMapping("/api/tokens/refresh")
    public ResponseEntity<?> forceRefreshTokens() {
        Map<String, Object> result = authService.forceRefreshAllTokens();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", result,
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    /**
     * Check if token refresh service is running
     */
    @GetMapping("/api/tokens/health")
    public ResponseEntity<?> getServiceHealth() {
        boolean isRunning = authService.isRunning();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                        "isRunning", isRunning,
                        "service", "TokenRefreshService",
                        "uptime", isRunning ? "Active" : "Inactive"),
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }
}
