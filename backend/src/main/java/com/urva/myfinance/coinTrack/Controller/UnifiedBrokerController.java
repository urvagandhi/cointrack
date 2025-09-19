package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.Service.AngelOneService;
import com.urva.myfinance.coinTrack.Service.UpstoxService;
import com.urva.myfinance.coinTrack.Service.ZerodhaService;
import com.urva.myfinance.coinTrack.Service.DataStandardizationService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UnifiedBrokerController {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedBrokerController.class);

    @Autowired
    private ZerodhaService zerodhaService;

    @Autowired
    private AngelOneService angelOneService;

    @Autowired
    private UpstoxService upstoxService;

    @Autowired
    private DataStandardizationService dataStandardizationService;

    /**
     * Get all orders from all broker accounts
     * Standardized format: /api/orders?broker=all|zerodha|angelone|upstox
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(
            @RequestParam(value = "broker", defaultValue = "all") String broker,
            @RequestParam(value = "status", required = false) String status) {

        try {
            logger.info("Fetching orders for broker: {}, status: {}", broker, status);

            List<DataStandardizationService.StandardOrder> allOrders = new java.util.ArrayList<>();
            String defaultUserId = "defaultUser"; // This should come from authentication context

            if ("all".equals(broker) || "zerodha".equals(broker)) {
                try {
                    Object zerodhaOrders = zerodhaService.getOrders(defaultUserId);
                    List<DataStandardizationService.StandardOrder> standardizedOrders = dataStandardizationService
                            .transformOrders(zerodhaOrders, "zerodha");
                    allOrders.addAll(standardizedOrders);
                } catch (IOException | com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException e) {
                    logger.warn("Failed to fetch Zerodha orders", e);
                } catch (Exception e) {
                    logger.warn("Failed to fetch Zerodha orders", e);
                }
            }

            if ("all".equals(broker) || "angelone".equals(broker)) {
                try {
                    Object angelOneOrders = angelOneService.getOrders(defaultUserId);
                    List<DataStandardizationService.StandardOrder> standardizedOrders = dataStandardizationService
                            .transformOrders(angelOneOrders, "angelone");
                    allOrders.addAll(standardizedOrders);
                } catch (Exception e) {
                    logger.warn("Failed to fetch Angel One orders", e);
                }
            }

            if ("all".equals(broker) || "upstox".equals(broker)) {
                try {
                    Object upstoxOrders = upstoxService.getOrders(defaultUserId);
                    List<DataStandardizationService.StandardOrder> standardizedOrders = dataStandardizationService
                            .transformOrders(upstoxOrders, "upstox");
                    allOrders.addAll(standardizedOrders);
                } catch (Exception e) {
                    logger.warn("Failed to fetch Upstox orders", e);
                }
            }

            // Filter by status if provided
            if (status != null && !status.isEmpty()) {
                allOrders = allOrders.stream()
                        .filter(order -> status.equalsIgnoreCase(order.status))
                        .collect(java.util.stream.Collectors.toList());
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", allOrders,
                    "meta", Map.of(
                            "total", allOrders.size(),
                            "broker_filter", broker,
                            "status_filter", status != null ? status : "all",
                            "generated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));

        } catch (Exception e) {
            logger.error("Error fetching orders", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch orders: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get all holdings from all broker accounts
     * Standardized format: /api/holdings?broker=all|zerodha|angelone|upstox
     */
    @GetMapping("/holdings")
    public ResponseEntity<?> getHoldings(
            @RequestParam(value = "broker", defaultValue = "all") String broker) {

        try {
            logger.info("Fetching holdings for broker: {}", broker);

            List<DataStandardizationService.StandardHolding> allHoldings = new java.util.ArrayList<>();
            String defaultUserId = "defaultUser"; // This should come from authentication context

            if ("all".equals(broker) || "zerodha".equals(broker)) {
                try {
                    Object zerodhaHoldings = zerodhaService.getHoldings(defaultUserId);
                    List<DataStandardizationService.StandardHolding> standardizedHoldings = dataStandardizationService
                            .transformHoldings(zerodhaHoldings, "zerodha");
                    allHoldings.addAll(standardizedHoldings);
                } catch (IOException | com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException e) {
                    logger.warn("Failed to fetch Zerodha holdings", e);
                } catch (Exception e) {
                    logger.warn("Failed to fetch Zerodha holdings", e);
                }
            }

            if ("all".equals(broker) || "angelone".equals(broker)) {
                try {
                    Object angelOneHoldings = angelOneService.getHoldings(defaultUserId);
                    List<DataStandardizationService.StandardHolding> standardizedHoldings = dataStandardizationService
                            .transformHoldings(angelOneHoldings, "angelone");
                    allHoldings.addAll(standardizedHoldings);
                } catch (Exception e) {
                    logger.warn("Failed to fetch Angel One holdings", e);
                }
            }

            if ("all".equals(broker) || "upstox".equals(broker)) {
                try {
                    Object upstoxHoldings = upstoxService.getHoldings(defaultUserId);
                    List<DataStandardizationService.StandardHolding> standardizedHoldings = dataStandardizationService
                            .transformHoldings(upstoxHoldings, "upstox");
                    allHoldings.addAll(standardizedHoldings);
                } catch (Exception e) {
                    logger.warn("Failed to fetch Upstox holdings", e);
                }
            }

            // Calculate total portfolio value
            double totalValue = allHoldings.stream()
                    .mapToDouble(holding -> holding.totalValue != null ? holding.totalValue : 0.0)
                    .sum();

            double totalPnL = allHoldings.stream()
                    .mapToDouble(holding -> holding.pnl != null ? holding.pnl : 0.0)
                    .sum();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", allHoldings,
                    "meta", Map.of(
                            "total_holdings", allHoldings.size(),
                            "total_value", totalValue,
                            "total_pnl", totalPnL,
                            "broker_filter", broker,
                            "generated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));

        } catch (Exception e) {
            logger.error("Error fetching holdings", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch holdings: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get all positions from all broker accounts
     * Standardized format: /api/positions?broker=all|zerodha|angelone|upstox
     */
    @GetMapping("/positions")
    public ResponseEntity<?> getPositions(
            @RequestParam(value = "broker", defaultValue = "all") String broker) {

        try {
            logger.info("Fetching positions for broker: {}", broker);

            List<DataStandardizationService.StandardPosition> allPositions = new java.util.ArrayList<>();
            String defaultUserId = "defaultUser"; // This should come from authentication context

            if ("all".equals(broker) || "zerodha".equals(broker)) {
                try {
                    Object zerodhaPositions = zerodhaService.getPositions(defaultUserId);
                    List<DataStandardizationService.StandardPosition> standardizedPositions = dataStandardizationService
                            .transformPositions(zerodhaPositions, "zerodha");
                    allPositions.addAll(standardizedPositions);
                } catch (IOException | com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException e) {
                    logger.warn("Failed to fetch Zerodha positions", e);
                } catch (Exception e) {
                    logger.warn("Failed to fetch Zerodha positions", e);
                }
            }

            if ("all".equals(broker) || "angelone".equals(broker)) {
                try {
                    Object angelOnePositions = angelOneService.getPositions(defaultUserId);
                    List<DataStandardizationService.StandardPosition> standardizedPositions = dataStandardizationService
                            .transformPositions(angelOnePositions, "angelone");
                    allPositions.addAll(standardizedPositions);
                } catch (Exception e) {
                    logger.warn("Failed to fetch Angel One positions", e);
                }
            }

            if ("all".equals(broker) || "upstox".equals(broker)) {
                try {
                    Object upstoxPositions = upstoxService.getPositions(defaultUserId);
                    List<DataStandardizationService.StandardPosition> standardizedPositions = dataStandardizationService
                            .transformPositions(upstoxPositions, "upstox");
                    allPositions.addAll(standardizedPositions);
                } catch (Exception e) {
                    logger.warn("Failed to fetch Upstox positions", e);
                }
            }

            // Calculate total P&L
            double totalPnL = allPositions.stream()
                    .mapToDouble(position -> position.pnl != null ? position.pnl : 0.0)
                    .sum();

            // Calculate total market value based on LTP and quantity
            double totalValue = allPositions.stream()
                    .mapToDouble(position -> {
                        if (position.ltp != null && position.quantity != null) {
                            return position.ltp * position.quantity;
                        }
                        return 0.0;
                    })
                    .sum();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", allPositions,
                    "meta", Map.of(
                            "total_positions", allPositions.size(),
                            "total_value", totalValue,
                            "total_pnl", totalPnL,
                            "broker_filter", broker,
                            "generated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));

        } catch (Exception e) {
            logger.error("Error fetching positions", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch positions: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get broker account status and connectivity
     * Standardized format: /api/broker-status
     */
    @GetMapping("/broker-status")
    public ResponseEntity<?> getBrokerStatus() {
        try {
            String defaultUserId = "defaultUser";
            Map<String, Object> brokerStatus = new java.util.HashMap<>();

            // Check Zerodha status
            try {
                zerodhaService.getAccountByAppUserId(defaultUserId);
                brokerStatus.put("zerodha", Map.of("status", "connected", "last_updated", LocalDateTime.now()));
            } catch (Exception e) {
                brokerStatus.put("zerodha", Map.of("status", "disconnected", "error", e.getMessage()));
            }

            // Check Angel One status
            try {
                angelOneService.getProfile(defaultUserId);
                brokerStatus.put("angelone", Map.of("status", "connected", "last_updated", LocalDateTime.now()));
            } catch (Exception e) {
                brokerStatus.put("angelone", Map.of("status", "disconnected", "error", e.getMessage()));
            }

            // Check Upstox status
            try {
                upstoxService.getProfile(defaultUserId);
                brokerStatus.put("upstox", Map.of("status", "connected", "last_updated", LocalDateTime.now()));
            } catch (Exception e) {
                brokerStatus.put("upstox", Map.of("status", "disconnected", "error", e.getMessage()));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", brokerStatus,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error checking broker status", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to check broker status: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }
}