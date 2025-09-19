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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.Service.TokenRefreshService;
import com.urva.myfinance.coinTrack.Service.WebSocketManager;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemController {

    private static final Logger logger = LoggerFactory.getLogger(SystemController.class);

    @Autowired
    private WebSocketManager webSocketManager;

    @Autowired
    private TokenRefreshService tokenRefreshService;

    /**
     * Get overall system status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getSystemStatus() {
        try {
            // Get WebSocket Manager status
            Map<String, Object> wsStatus = webSocketManager.getConnectionStatus();

            // Get Token Refresh Service status
            Map<String, Object> tokenStatus = tokenRefreshService.getTokenRefreshStatus();

            // Get market statistics
            Map<String, Object> marketStats = webSocketManager.getMarketStatistics();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "webSocketManager", wsStatus,
                            "tokenRefreshService", tokenStatus,
                            "marketStatistics", marketStats,
                            "systemUptime", "Active",
                            "version", "1.0.0"),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error getting system status", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to get system status: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get health check for all services
     */
    @GetMapping("/health")
    public ResponseEntity<?> getHealthCheck() {
        try {
            boolean wsManagerHealthy = webSocketManager.getConnectionStatus() != null;
            boolean tokenServiceHealthy = tokenRefreshService.isRunning();

            Map<String, Object> wsConnStatus = webSocketManager.getConnectionStatus();
            boolean nseConnected = (Boolean) wsConnStatus.get("nseConnected");
            boolean bseConnected = (Boolean) wsConnStatus.get("bseConnected");

            String overallHealth = (wsManagerHealthy && tokenServiceHealthy &&
                    (nseConnected || bseConnected)) ? "HEALTHY" : "DEGRADED";

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "overallHealth", overallHealth,
                            "services", Map.of(
                                    "webSocketManager", wsManagerHealthy ? "UP" : "DOWN",
                                    "tokenRefreshService", tokenServiceHealthy ? "UP" : "DOWN",
                                    "nseConnection", nseConnected ? "CONNECTED" : "DISCONNECTED",
                                    "bseConnection", bseConnected ? "CONNECTED" : "DISCONNECTED"),
                            "brokers", Map.of(
                                    "zerodha", "INTEGRATED",
                                    "angelone", "INTEGRATED",
                                    "upstox", "INTEGRATED")),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error performing health check", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Health check failed: " + e.getMessage(),
                    "overallHealth", "ERROR",
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get system configuration and features
     */
    @GetMapping("/info")
    public ResponseEntity<?> getSystemInfo() {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "application", Map.of(
                                    "name", "CoinTrack Finance Dashboard",
                                    "version", "1.0.0",
                                    "description", "Live stock market system with unified broker integration"),
                            "features", Map.of(
                                    "liveMarketData", true,
                                    "multiExchangeSupport", true,
                                    "realTimeStreaming", true,
                                    "brokerIntegration", true,
                                    "tokenRefresh", true,
                                    "dataStandardization", true),
                            "supportedBrokers", Map.of(
                                    "zerodha", Map.of(
                                            "name", "Zerodha Kite",
                                            "authType", "API_KEY",
                                            "features",
                                            new String[] { "orders", "holdings", "positions", "historical" }),
                                    "angelone", Map.of(
                                            "name", "Angel One SmartAPI",
                                            "authType", "JWT",
                                            "features",
                                            new String[] { "orders", "holdings", "positions", "historical", "ltp" }),
                                    "upstox", Map.of(
                                            "name", "Upstox API",
                                            "authType", "OAUTH2",
                                            "features",
                                            new String[] { "orders", "holdings", "positions", "historical" })),
                            "exchanges", Map.of(
                                    "nse", Map.of(
                                            "name", "National Stock Exchange",
                                            "country", "India",
                                            "liveData", true),
                                    "bse", Map.of(
                                            "name", "Bombay Stock Exchange",
                                            "country", "India",
                                            "liveData", true)),
                            "apis", Map.of(
                                    "liveMarketData", "/api/live/*",
                                    "brokerOrders", "/api/*/orders",
                                    "brokerHoldings", "/api/*/holdings",
                                    "brokerPositions", "/api/*/positions",
                                    "systemStatus", "/api/system/status",
                                    "tokenManagement", "/api/tokens/*")),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error getting system info", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to get system info: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }
}