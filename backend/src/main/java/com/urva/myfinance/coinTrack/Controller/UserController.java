package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.urva.myfinance.coinTrack.Service.UserService;
import com.urva.myfinance.coinTrack.Service.ZerodhaService;
import com.urva.myfinance.coinTrack.Service.AngelOneService;
import com.urva.myfinance.coinTrack.Service.UpstoxService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

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
        try {
            List<User> users = service.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching users: " + e.getMessage());
        }
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        try {
            User user = service.getUserById(id);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found with id: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching user: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            User registeredUser = service.registerUser(user);
            if (registeredUser != null) {
                return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Failed to register user. Please check username and password are provided.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering user: " + e.getMessage());
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody User user) {
        try {
            User updatedUser = service.updateUser(id, user);
            if (updatedUser != null) {
                return ResponseEntity.ok(updatedUser);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found with id: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating user: " + e.getMessage());
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            boolean deleted = service.deleteUser(id);
            if (deleted) {
                return ResponseEntity.ok("User deleted successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found with id: " + id);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting user: " + e.getMessage());
        }
    }

    @GetMapping("/users/verify")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            boolean isValid = service.isTokenValid(token);

            if (!isValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid or expired token");
            }

            User user = service.getUserByToken(token);
            if (user != null) {
                user.setPassword(null);
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error verifying token: " + e.getMessage());
        }
    }

    /**
     * Get user's broker accounts
     * Standardized format: /api/users/:id/brokers
     */
    @GetMapping("/users/{id}/brokers")
    public ResponseEntity<?> getUserBrokerAccounts(@PathVariable String id) {
        try {
            User user = service.getUserById(id);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "error",
                        "message", "User not found",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            Map<String, Object> brokerAccounts = new java.util.HashMap<>();

            // Check Zerodha account
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

            // Check Angel One account
            try {
                AngelOneAccount angelOneAccount = angelOneService.getAccountByAppUserId(id);
                brokerAccounts.put("angelone", Map.of(
                        "status", "connected",
                        "account_id",
                        angelOneAccount.getAngelClientId() != null ? angelOneAccount.getAngelClientId() : "N/A",
                        "api_key_set", angelOneAccount.getAngelApiKey() != null,
                        "connected_at", angelOneAccount.getCreatedAt()));
            } catch (Exception e) {
                brokerAccounts.put("angelone", Map.of(
                        "status", "not_connected",
                        "reason", e.getMessage()));
            }

            // Check Upstox account
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

        } catch (Exception e) {
            logger.error("Error fetching broker accounts for user: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch broker accounts: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Link Zerodha account to user
     * Standardized format: /api/users/:id/brokers/zerodha (POST)
     */
    @PostMapping("/users/{id}/brokers/zerodha")
    public ResponseEntity<?> linkZerodhaAccount(@PathVariable String id, @RequestBody Map<String, String> credentials) {
        try {
            User user = service.getUserById(id);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "error",
                        "message", "User not found",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            String apiKey = credentials.get("api_key");
            String apiSecret = credentials.get("api_secret");

            if (apiKey == null || apiSecret == null) {
                return ResponseEntity.status(400).body(Map.of(
                        "status", "error",
                        "message", "API key and secret are required",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
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

        } catch (Exception e) {
            logger.error("Error linking Zerodha account for user: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to link Zerodha account: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Link Angel One account to user
     * Standardized format: /api/users/:id/brokers/angelone (POST)
     */
    @PostMapping("/users/{id}/brokers/angelone")
    public ResponseEntity<?> linkAngelOneAccount(@PathVariable String id,
            @RequestBody Map<String, String> credentials) {
        try {
            User user = service.getUserById(id);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "error",
                        "message", "User not found",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            String clientId = credentials.get("client_id");
            String apiKey = credentials.get("api_key");
            String password = credentials.get("password");

            if (clientId == null || apiKey == null || password == null) {
                return ResponseEntity.status(400).body(Map.of(
                        "status", "error",
                        "message", "Client ID, API key, and password are required",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
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

        } catch (Exception e) {
            logger.error("Error linking Angel One account for user: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to link Angel One account: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Link Upstox account to user
     * Standardized format: /api/users/:id/brokers/upstox (POST)
     */
    @PostMapping("/users/{id}/brokers/upstox")
    public ResponseEntity<?> linkUpstoxAccount(@PathVariable String id, @RequestBody Map<String, String> credentials) {
        try {
            User user = service.getUserById(id);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                        "status", "error",
                        "message", "User not found",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            String clientId = credentials.get("client_id");
            String apiKey = credentials.get("api_key");
            String apiSecret = credentials.get("api_secret");
            String redirectUri = credentials.get("redirect_uri");

            if (clientId == null || apiKey == null || apiSecret == null) {
                return ResponseEntity.status(400).body(Map.of(
                        "status", "error",
                        "message", "Client ID, API key, and API secret are required",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
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

        } catch (Exception e) {
            logger.error("Error linking Upstox account for user: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to link Upstox account: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }
}
