package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.Model.AngelOneAccount;
import com.urva.myfinance.coinTrack.Model.UpstoxAccount;
import com.urva.myfinance.coinTrack.Model.ZerodhaAccount;
import com.urva.myfinance.coinTrack.Repository.AngelOneAccountRepository;
import com.urva.myfinance.coinTrack.Repository.UpstoxAccountRepository;
import com.urva.myfinance.coinTrack.Repository.ZerodhaAccountRepository;
import com.urva.myfinance.coinTrack.Service.AngelOneService;
import com.urva.myfinance.coinTrack.Service.UpstoxService;
import com.urva.myfinance.coinTrack.Service.ZerodhaService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

@RestController
@RequestMapping("/api/brokers")
@CrossOrigin(origins = "*")
public class BrokerManagementController {

    private static final Logger logger = LoggerFactory.getLogger(BrokerManagementController.class);

    @Autowired
    private ZerodhaAccountRepository zerodhaAccountRepository;

    @Autowired
    private AngelOneAccountRepository angelOneAccountRepository;

    @Autowired
    private UpstoxAccountRepository upstoxAccountRepository;

    @Autowired
    private ZerodhaService zerodhaService;

    @Autowired
    private AngelOneService angelOneService;

    @Autowired
    private UpstoxService upstoxService;

    /**
     * Get all broker accounts for a user
     * GET /api/brokers/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserBrokerAccounts(@PathVariable String userId) {
        try {
            // Fetch all broker accounts for the user
            Optional<ZerodhaAccount> zerodhaAccount = zerodhaAccountRepository.findByAppUserId(userId);
            Optional<AngelOneAccount> angelOneAccount = angelOneAccountRepository.findByAppUserId(userId);
            Optional<UpstoxAccount> upstoxAccount = upstoxAccountRepository.findByAppUserId(userId);

            List<Map<String, Object>> brokerAccounts = List.of(
                    Map.of(
                            "broker", "zerodha",
                            "connected", zerodhaAccount.isPresent(),
                            "account_info", zerodhaAccount.map(this::mapZerodhaAccount).orElse(null)),
                    Map.of(
                            "broker", "angelone",
                            "connected", angelOneAccount.isPresent(),
                            "account_info", angelOneAccount.map(this::mapAngelOneAccount).orElse(null)),
                    Map.of(
                            "broker", "upstox",
                            "connected", upstoxAccount.isPresent(),
                            "account_info", upstoxAccount.map(this::mapUpstoxAccount).orElse(null)));

            long connectedCount = brokerAccounts.stream()
                    .mapToLong(broker -> (Boolean) broker.get("connected") ? 1 : 0)
                    .sum();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "user_id", userId,
                            "brokers", brokerAccounts,
                            "total_brokers", 3,
                            "connected_brokers", connectedCount),
                    "message", "Broker accounts retrieved successfully"));
        } catch (Exception e) {
            logger.error("Error fetching broker accounts for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to fetch broker accounts: " + e.getMessage()));
        }
    }

    /**
     * Get broker status and health check
     * GET /api/brokers/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBrokerStatus() {
        try {
            // Run health checks for all brokers in parallel
            CompletableFuture<Map<String, Object>> zerodhaStatus = CompletableFuture.supplyAsync(() -> {
                try {
                    // You can implement a health check method in each service
                    return Map.of(
                            "broker", "zerodha",
                            "status", "active",
                            "last_check", LocalDateTime.now(),
                            "connected_accounts", zerodhaAccountRepository.count());
                } catch (Exception e) {
                    return Map.of(
                            "broker", "zerodha",
                            "status", "error",
                            "error", e.getMessage(),
                            "last_check", LocalDateTime.now());
                }
            });

            CompletableFuture<Map<String, Object>> angelOneStatus = CompletableFuture.supplyAsync(() -> {
                try {
                    return Map.of(
                            "broker", "angelone",
                            "status", "active",
                            "last_check", LocalDateTime.now(),
                            "connected_accounts", angelOneAccountRepository.count());
                } catch (Exception e) {
                    return Map.of(
                            "broker", "angelone",
                            "status", "error",
                            "error", e.getMessage(),
                            "last_check", LocalDateTime.now());
                }
            });

            CompletableFuture<Map<String, Object>> upstoxStatus = CompletableFuture.supplyAsync(() -> {
                try {
                    return Map.of(
                            "broker", "upstox",
                            "status", "active",
                            "last_check", LocalDateTime.now(),
                            "connected_accounts", upstoxAccountRepository.count());
                } catch (Exception e) {
                    return Map.of(
                            "broker", "upstox",
                            "status", "error",
                            "error", e.getMessage(),
                            "last_check", LocalDateTime.now());
                }
            });

            // Wait for all status checks to complete
            CompletableFuture.allOf(zerodhaStatus, angelOneStatus, upstoxStatus).join();

            List<Map<String, Object>> brokerStatusList = List.of(
                    zerodhaStatus.get(),
                    angelOneStatus.get(),
                    upstoxStatus.get());

            long activeBrokers = brokerStatusList.stream()
                    .mapToLong(status -> "active".equals(status.get("status")) ? 1 : 0)
                    .sum();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "brokers", brokerStatusList,
                            "total_brokers", 3,
                            "active_brokers", activeBrokers,
                            "system_status", activeBrokers > 0 ? "operational" : "degraded"),
                    "message", "Broker status retrieved successfully"));
        } catch (Exception e) {
            logger.error("Error fetching broker status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to fetch broker status: " + e.getMessage()));
        }
    }

    /**
     * Test broker connection
     * POST /api/brokers/{broker}/test/{userId}
     * 
     * @throws KiteException
     */
    @PostMapping("/{broker}/test/{userId}")
    public ResponseEntity<Map<String, Object>> testBrokerConnection(
            @PathVariable String broker,
            @PathVariable String userId) throws KiteException {
        try {
            Map<String, Object> testResult;

            switch (broker.toLowerCase()) {
                case "zerodha":
                    testResult = testZerodhaConnection(userId);
                    break;
                case "angelone":
                    testResult = testAngelOneConnection(userId);
                    break;
                case "upstox":
                    testResult = testUpstoxConnection(userId);
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                    "status", "error",
                                    "message", "Invalid broker: " + broker));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", testResult,
                    "message", "Broker connection test completed"));
        } catch (Exception e) {
            logger.error("Error testing {} connection for user {}: {}", broker, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to test broker connection: " + e.getMessage()));
        }
    }

    /**
     * Disconnect broker account
     * DELETE /api/brokers/{broker}/{userId}
     */
    @DeleteMapping("/{broker}/{userId}")
    public ResponseEntity<Map<String, Object>> disconnectBrokerAccount(
            @PathVariable String broker,
            @PathVariable String userId) {
        try {
            boolean disconnected = false;

            switch (broker.toLowerCase()) {
                case "zerodha":
                    Optional<ZerodhaAccount> zerodhaAccount = zerodhaAccountRepository.findByAppUserId(userId);
                    if (zerodhaAccount.isPresent()) {
                        zerodhaAccountRepository.delete(zerodhaAccount.get());
                        disconnected = true;
                    }
                    break;
                case "angelone":
                    Optional<AngelOneAccount> angelOneAccount = angelOneAccountRepository.findByAppUserId(userId);
                    if (angelOneAccount.isPresent()) {
                        angelOneAccountRepository.delete(angelOneAccount.get());
                        disconnected = true;
                    }
                    break;
                case "upstox":
                    Optional<UpstoxAccount> upstoxAccount = upstoxAccountRepository.findByAppUserId(userId);
                    if (upstoxAccount.isPresent()) {
                        upstoxAccountRepository.delete(upstoxAccount.get());
                        disconnected = true;
                    }
                    break;
                default:
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                    "status", "error",
                                    "message", "Invalid broker: " + broker));
            }

            if (!disconnected) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", "error",
                                "message", "Broker account not found for user"));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", broker + " account disconnected successfully"));
        } catch (Exception e) {
            logger.error("Error disconnecting {} for user {}: {}", broker, userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to disconnect broker account: " + e.getMessage()));
        }
    }

    // Helper methods for mapping account data
    private Map<String, Object> mapZerodhaAccount(ZerodhaAccount account) {
        return Map.of(
                "app_user_id", account.getAppUserId(),
                "kite_user_id", account.getKiteUserId(),
                "created_at", account.getCreatedAt(),
                "last_updated", account.getUpdatedAt());
    }

    private Map<String, Object> mapAngelOneAccount(AngelOneAccount account) {
        return Map.of(
                "app_user_id", account.getAppUserId(),
                "angel_client_id", account.getAngelClientId(),
                "user_id", account.getUserId(),
                "created_at", account.getCreatedAt(),
                "last_updated", account.getUpdatedAt());
    }

    private Map<String, Object> mapUpstoxAccount(UpstoxAccount account) {
        return Map.of(
                "app_user_id", account.getAppUserId(),
                "user_id", account.getUserId(),
                "user_name", account.getUserName(),
                "created_at", account.getCreatedAt(),
                "last_updated", account.getUpdatedAt());
    }

    // Helper methods for testing broker connections
    private Map<String, Object> testZerodhaConnection(String userId) throws KiteException {
        try {
            Optional<ZerodhaAccount> account = zerodhaAccountRepository.findByAppUserId(userId);
            if (account.isEmpty()) {
                return Map.of(
                        "broker", "zerodha",
                        "connected", false,
                        "status", "not_configured",
                        "message", "Account not configured");
            }

            // Test by fetching holdings (simplified test)
            Object holdings = zerodhaService.getHoldings(userId);
            return Map.of(
                    "broker", "zerodha",
                    "connected", true,
                    "status", "active",
                    "message", "Connection successful",
                    "test_time", LocalDateTime.now(),
                    "test_data", holdings != null ? "Holdings fetched" : "No holdings");
        } catch (Exception e) {
            return Map.of(
                    "broker", "zerodha",
                    "connected", false,
                    "status", "error",
                    "message", "Connection failed: " + e.getMessage(),
                    "test_time", LocalDateTime.now());
        }
    }

    private Map<String, Object> testAngelOneConnection(String userId) {
        try {
            Optional<AngelOneAccount> account = angelOneAccountRepository.findByAppUserId(userId);
            if (account.isEmpty()) {
                return Map.of(
                        "broker", "angelone",
                        "connected", false,
                        "status", "not_configured",
                        "message", "Account not configured");
            }

            // Test by fetching profile
            Object profile = angelOneService.getProfile(userId);
            return Map.of(
                    "broker", "angelone",
                    "connected", true,
                    "status", "active",
                    "message", "Connection successful",
                    "test_time", LocalDateTime.now(),
                    "account_info", profile);
        } catch (Exception e) {
            return Map.of(
                    "broker", "angelone",
                    "connected", false,
                    "status", "error",
                    "message", "Connection failed: " + e.getMessage(),
                    "test_time", LocalDateTime.now());
        }
    }

    private Map<String, Object> testUpstoxConnection(String userId) {
        try {
            Optional<UpstoxAccount> account = upstoxAccountRepository.findByAppUserId(userId);
            if (account.isEmpty()) {
                return Map.of(
                        "broker", "upstox",
                        "connected", false,
                        "status", "not_configured",
                        "message", "Account not configured");
            }

            // Test by fetching profile
            Object profile = upstoxService.getProfile(userId);
            return Map.of(
                    "broker", "upstox",
                    "connected", true,
                    "status", "active",
                    "message", "Connection successful",
                    "test_time", LocalDateTime.now(),
                    "account_info", profile);
        } catch (Exception e) {
            return Map.of(
                    "broker", "upstox",
                    "connected", false,
                    "status", "error",
                    "message", "Connection failed: " + e.getMessage(),
                    "test_time", LocalDateTime.now());
        }
    }
}