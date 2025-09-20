package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.Model.User;
import com.urva.myfinance.coinTrack.Model.ZerodhaAccount;
import com.urva.myfinance.coinTrack.Model.AngelOneAccount;
import com.urva.myfinance.coinTrack.Model.UpstoxAccount;
import com.urva.myfinance.coinTrack.ResourceNotFoundException;
import com.urva.myfinance.coinTrack.UnauthorizedException;
import com.urva.myfinance.coinTrack.Service.UserService;
import com.urva.myfinance.coinTrack.Service.ZerodhaService;
import com.urva.myfinance.coinTrack.Service.AngelOneService;
import com.urva.myfinance.coinTrack.Service.UpstoxService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class UserController {

    private final UserService service;

    @Autowired
    private ZerodhaService zerodhaService;

    @Autowired
    private AngelOneService angelOneService;

    @Autowired
    private UpstoxService upstoxService;

    public UserController(UserService service) {
        this.service = service;
    }

    @RequestMapping("/users")
    public ResponseEntity<?> getUsers() {
        List<User> users = service.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        User user = service.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        User registeredUser = service.registerUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody User user) {
        User updatedUser = service.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        service.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }

    @GetMapping("/users/verify")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        boolean isValid = service.isTokenValid(token);

        if (!isValid) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        User user = service.getUserByToken(token);
        if (user == null) {
            throw new ResourceNotFoundException("User not found for the provided token");
        }
        
        // Remove password for security
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    /**
     * Get user's broker accounts
     * Standardized format: /api/users/:id/brokers
     */
    @GetMapping("/users/{id}/brokers")
    public ResponseEntity<?> getUserBrokerAccounts(@PathVariable String id) {
        User user = service.getUserById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        Map<String, Object> brokerAccounts = new java.util.HashMap<>();

        // Check Zerodha account - service should handle exceptions internally
        try {
            ZerodhaAccount zerodhaAccount = zerodhaService.getAccountByAppUserId(id);
            brokerAccounts.put("zerodha", Map.of(
                    "status", "connected",
                    "account_id", zerodhaAccount.getKiteUserId() != null ? zerodhaAccount.getKiteUserId() : "N/A",
                    "api_key_set", zerodhaAccount.getZerodhaApiKey() != null,
                    "connected_at", zerodhaAccount.getCreatedAt()));
        } catch (Exception e) {
            brokerAccounts.put("zerodha", Map.of(
                    "status", "not_connected",
                    "reason", e.getMessage()));
        }

        // Check Angel One account - service should handle exceptions internally
        try {
            AngelOneAccount angelOneAccount = angelOneService.getAccountByAppUserId(id);
            brokerAccounts.put("angelone", Map.of(
                    "status", "connected",
                    "account_id", angelOneAccount.getAngelClientId() != null ? angelOneAccount.getAngelClientId() : "N/A",
                    "api_key_set", angelOneAccount.getAngelApiKey() != null,
                    "connected_at", angelOneAccount.getCreatedAt()));
        } catch (Exception e) {
            brokerAccounts.put("angelone", Map.of(
                    "status", "not_connected",
                    "reason", e.getMessage()));
        }

        // Check Upstox account - service should handle exceptions internally
        try {
            UpstoxAccount upstoxAccount = upstoxService.getAccountByAppUserId(id);
            brokerAccounts.put("upstox", Map.of(
                    "status", "connected",
                    "account_id", upstoxAccount.getUserId() != null ? upstoxAccount.getUserId() : "N/A",
                    "api_key_set", upstoxAccount.getUpstoxApiKey() != null,
                    "connected_at", upstoxAccount.getCreatedAt()));
        } catch (Exception e) {
            brokerAccounts.put("upstox", Map.of(
                    "status", "not_connected",
                    "reason", e.getMessage()));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", brokerAccounts,
                "user_id", id,
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    /**
     * Link Zerodha account to user
     * Standardized format: /api/users/:id/brokers/zerodha (POST)
     */
    @PostMapping("/users/{id}/brokers/zerodha")
    public ResponseEntity<?> linkZerodhaAccount(@PathVariable String id, @RequestBody Map<String, String> credentials) {
        User user = service.getUserById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        String apiKey = credentials.get("api_key");
        String apiSecret = credentials.get("api_secret");

        if (apiKey == null || apiSecret == null) {
            throw new IllegalArgumentException("API key and secret are required");
        }

        ZerodhaAccount account = zerodhaService.setZerodhaCredentials(id, apiKey, apiSecret);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                        "broker", "zerodha",
                        "account_id", account.getKiteUserId(),
                        "connected", true),
                "message", "Zerodha account linked successfully",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    /**
     * Link Angel One account to user
     * Standardized format: /api/users/:id/brokers/angelone (POST)
     */
    @PostMapping("/users/{id}/brokers/angelone")
    public ResponseEntity<?> linkAngelOneAccount(@PathVariable String id, @RequestBody Map<String, String> credentials) {
        User user = service.getUserById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        String clientId = credentials.get("client_id");
        String apiKey = credentials.get("api_key");
        String password = credentials.get("password");

        if (clientId == null || apiKey == null || password == null) {
            throw new IllegalArgumentException("Client ID, API key, and password are required");
        }

        AngelOneAccount account = angelOneService.setAngelOneCredentials(id, apiKey, clientId, password);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                        "broker", "angelone",
                        "account_id", account.getAngelClientId(),
                        "connected", true),
                "message", "Angel One account linked successfully",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    /**
     * Link Upstox account to user
     * Standardized format: /api/users/:id/brokers/upstox (POST)
     */
    @PostMapping("/users/{id}/brokers/upstox")
    public ResponseEntity<?> linkUpstoxAccount(@PathVariable String id, @RequestBody Map<String, String> credentials) {
        User user = service.getUserById(id);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }

        String clientId = credentials.get("client_id");
        String apiKey = credentials.get("api_key");
        String apiSecret = credentials.get("api_secret");
        String redirectUri = credentials.get("redirect_uri");

        if (clientId == null || apiKey == null || apiSecret == null) {
            throw new IllegalArgumentException("Client ID, API key, and API secret are required");
        }

        UpstoxAccount account = upstoxService.setUpstoxCredentials(id, apiKey, apiSecret, redirectUri);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                        "broker", "upstox",
                        "account_id", account.getUserId(),
                        "connected", true),
                "message", "Upstox account linked successfully",
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }
}
