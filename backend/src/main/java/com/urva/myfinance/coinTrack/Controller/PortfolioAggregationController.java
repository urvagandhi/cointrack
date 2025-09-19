package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

import com.urva.myfinance.coinTrack.Service.ZerodhaService;
import com.urva.myfinance.coinTrack.Service.AngelOneService;
import com.urva.myfinance.coinTrack.Service.UpstoxService;
import com.urva.myfinance.coinTrack.Service.DataStandardizationService;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioAggregationController {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioAggregationController.class);

    @Autowired
    private ZerodhaService zerodhaService;

    @Autowired
    private AngelOneService angelOneService;

    @Autowired
    private UpstoxService upstoxService;

    @Autowired
    private DataStandardizationService dataStandardizationService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    /**
     * Get aggregated portfolio value across all broker accounts
     * Enhanced with MF, SIP, and Orders data
     */
    @GetMapping("/value")
    public ResponseEntity<?> getPortfolioValue(
            @RequestParam(value = "user_id", defaultValue = "defaultUser") String userId,
            @RequestParam(value = "include", defaultValue = "holdings,positions,mf,sip,orders") String include,
            @RequestParam(value = "broker", defaultValue = "all") String broker) {

        try {
            logger.info("Fetching enhanced portfolio value for user: {}, include: {}, broker: {}", userId, include,
                    broker);

            boolean includeHoldings = include.contains("holdings");
            boolean includePositions = include.contains("positions");
            boolean includeMF = include.contains("mf");
            boolean includeSIP = include.contains("sip");
            boolean includeOrders = include.contains("orders");

            // Parallel fetch from all brokers
            CompletableFuture<Map<String, Object>> zerodhaFuture = CompletableFuture.supplyAsync(
                    () -> fetchEnhancedBrokerPortfolio("zerodha", userId, includeHoldings, includePositions,
                            includeMF, includeSIP, includeOrders, broker),
                    executorService);

            CompletableFuture<Map<String, Object>> angelOneFuture = CompletableFuture.supplyAsync(
                    () -> fetchEnhancedBrokerPortfolio("angelone", userId, includeHoldings, includePositions,
                            includeMF, includeSIP, includeOrders, broker),
                    executorService);

            CompletableFuture<Map<String, Object>> upstoxFuture = CompletableFuture.supplyAsync(
                    () -> fetchEnhancedBrokerPortfolio("upstox", userId, includeHoldings, includePositions,
                            includeMF, includeSIP, includeOrders, broker),
                    executorService);

            // Wait for all futures to complete
            CompletableFuture.allOf(zerodhaFuture, angelOneFuture, upstoxFuture).join();

            // Collect results
            Map<String, Object> zerodhaData = zerodhaFuture.get();
            Map<String, Object> angelOneData = angelOneFuture.get();
            Map<String, Object> upstoxData = upstoxFuture.get();

            // Aggregate portfolio data
            Map<String, Object> aggregatedPortfolio = aggregateEnhancedPortfolioData(
                    zerodhaData, angelOneData, upstoxData, includeHoldings, includePositions,
                    includeMF, includeSIP, includeOrders);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", aggregatedPortfolio,
                    "meta", Map.of(
                            "user_id", userId,
                            "included_data", include,
                            "broker_filter", broker,
                            "generated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));

        } catch (Exception e) {
            logger.error("Error fetching enhanced portfolio value for user: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch enhanced portfolio value: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get enhanced portfolio breakdown by broker including MF/SIP/Orders
     */
    @GetMapping("/breakdown")
    public ResponseEntity<?> getPortfolioBreakdown(
            @RequestParam(value = "user_id", defaultValue = "defaultUser") String userId,
            @RequestParam(value = "include", defaultValue = "holdings,positions,mf,sip,orders") String include) {

        try {
            logger.info("Fetching enhanced portfolio breakdown for user: {}", userId);

            boolean includeHoldings = include.contains("holdings");
            boolean includePositions = include.contains("positions");
            boolean includeMF = include.contains("mf");
            boolean includeSIP = include.contains("sip");
            boolean includeOrders = include.contains("orders");

            Map<String, Object> breakdown = new java.util.HashMap<>();

            // Fetch enhanced data from each broker
            Map<String, Object> zerodhaData = fetchEnhancedBrokerPortfolio("zerodha", userId,
                    includeHoldings, includePositions, includeMF, includeSIP, includeOrders, "zerodha");
            Map<String, Object> angelOneData = fetchEnhancedBrokerPortfolio("angelone", userId,
                    includeHoldings, includePositions, includeMF, includeSIP, includeOrders, "angelone");
            Map<String, Object> upstoxData = fetchEnhancedBrokerPortfolio("upstox", userId,
                    includeHoldings, includePositions, includeMF, includeSIP, includeOrders, "upstox");

            breakdown.put("zerodha", zerodhaData);
            breakdown.put("angelone", angelOneData);
            breakdown.put("upstox", upstoxData);

            // Calculate enhanced totals
            Map<String, Object> totalSummary = calculateEnhancedTotalSummary(
                    List.of(zerodhaData, angelOneData, upstoxData));
            breakdown.put("total_summary", totalSummary);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", breakdown,
                    "meta", Map.of(
                            "user_id", userId,
                            "included_data", include,
                            "generated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));

        } catch (Exception e) {
            logger.error("Error fetching enhanced portfolio breakdown for user: {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch enhanced portfolio breakdown: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Enhanced broker portfolio fetching with MF/SIP/Orders support
     */
    private Map<String, Object> fetchEnhancedBrokerPortfolio(String brokerName, String userId,
            boolean includeHoldings, boolean includePositions, boolean includeMF,
            boolean includeSIP, boolean includeOrders, String brokerFilter) {

        Map<String, Object> brokerData = new java.util.HashMap<>();

        try {
            // Skip if broker is not in filter
            if (!"all".equals(brokerFilter) && !brokerName.equals(brokerFilter)) {
                return Map.of(
                        "status", "skipped",
                        "broker", brokerName,
                        "summary", createEmptyEnhancedSummary());
            }

            // Initialize data collections
            List<DataStandardizationService.StandardHolding> holdings = List.of();
            List<DataStandardizationService.StandardPosition> positions = List.of();
            List<Map<String, Object>> mfHoldings = List.of();
            List<Map<String, Object>> sipOrders = List.of();
            List<Map<String, Object>> orders = List.of();

            // Fetch data based on include flags
            if (includeHoldings) {
                holdings = fetchHoldingsFromBroker(brokerName, userId);
            }

            if (includePositions) {
                positions = fetchPositionsFromBroker(brokerName, userId);
            }

            if (includeMF) {
                mfHoldings = fetchMFHoldingsFromBroker(brokerName, userId);
            }

            if (includeSIP) {
                sipOrders = fetchSIPOrdersFromBroker(brokerName, userId);
            }

            if (includeOrders) {
                orders = fetchOrdersFromBroker(brokerName, userId);
            }

            // Calculate enhanced summary
            Map<String, Object> summary = calculateEnhancedSummary(holdings, positions, mfHoldings, sipOrders, orders);

            brokerData.put("status", "success");
            brokerData.put("broker", brokerName);
            brokerData.put("holdings", holdings);
            brokerData.put("positions", positions);
            brokerData.put("mf_holdings", mfHoldings);
            brokerData.put("sip_orders", sipOrders);
            brokerData.put("orders", orders);
            brokerData.put("summary", summary);

        } catch (Exception e) {
            logger.error("Error fetching enhanced {} portfolio for user {}: {}", brokerName, userId, e.getMessage());
            brokerData.put("status", "error");
            brokerData.put("broker", brokerName);
            brokerData.put("error", e.getMessage());
            brokerData.put("summary", createEmptyEnhancedSummary());
        }

        return brokerData;
    }

    /**
     * Fetch holdings from specific broker
     */
    private List<DataStandardizationService.StandardHolding> fetchHoldingsFromBroker(String brokerName, String userId) {
        try {
            Object rawHoldings = null;
            switch (brokerName) {
                case "zerodha":
                    try {
                        rawHoldings = zerodhaService.getHoldings(userId);
                    } catch (IOException | com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException e) {
                        logger.warn("Failed to fetch Zerodha holdings: {}", e.getMessage());
                        return List.of();
                    }
                    break;
                case "angelone":
                    rawHoldings = angelOneService.getHoldings(userId);
                    break;
                case "upstox":
                    rawHoldings = upstoxService.getHoldings(userId);
                    break;
            }

            if (rawHoldings != null) {
                return dataStandardizationService.transformHoldings(rawHoldings, brokerName);
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch holdings from {}: {}", brokerName, e.getMessage());
        }
        return List.of();
    }

    /**
     * Fetch positions from specific broker
     */
    private List<DataStandardizationService.StandardPosition> fetchPositionsFromBroker(String brokerName,
            String userId) {
        try {
            Object rawPositions = null;
            switch (brokerName) {
                case "zerodha":
                    try {
                        rawPositions = zerodhaService.getPositions(userId);
                    } catch (IOException | com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException e) {
                        logger.warn("Failed to fetch Zerodha positions: {}", e.getMessage());
                        return List.of();
                    }
                    break;
                case "angelone":
                    rawPositions = angelOneService.getPositions(userId);
                    break;
                case "upstox":
                    rawPositions = upstoxService.getPositions(userId);
                    break;
            }

            if (rawPositions != null) {
                return dataStandardizationService.transformPositions(rawPositions, brokerName);
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch positions from {}: {}", brokerName, e.getMessage());
        }
        return List.of();
    }

    /**
     * Fetch Mutual Fund holdings from specific broker
     */
    private List<Map<String, Object>> fetchMFHoldingsFromBroker(String brokerName, String userId) {
        try {
            switch (brokerName) {
                case "zerodha":
                    // Zerodha doesn't support MF through Kite API
                    logger.info("MF holdings not available through Zerodha Kite API");
                    return List.of();
                case "angelone":
                    // Note: Angel One may have MF APIs - implement when available
                    logger.info("MF holdings not implemented for Angel One yet - API not available");
                    return List.of();
                case "upstox":
                    // Note: Upstox may have MF APIs - implement when available
                    logger.info("MF holdings not implemented for Upstox yet - API not available");
                    return List.of();
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch MF holdings from {}: {}", brokerName, e.getMessage());
        }
        return List.of();
    }

    /**
     * Fetch SIP orders from specific broker
     */
    private List<Map<String, Object>> fetchSIPOrdersFromBroker(String brokerName, String userId) {
        try {
            switch (brokerName) {
                case "zerodha":
                    // Zerodha doesn't support SIP through Kite API
                    logger.info("SIP orders not available through Zerodha Kite API");
                    return List.of();
                case "angelone":
                    // Note: Angel One may have SIP APIs - implement when available
                    logger.info("SIP orders not implemented for Angel One yet - API not available");
                    return List.of();
                case "upstox":
                    // Note: Upstox may have SIP APIs - implement when available
                    logger.info("SIP orders not implemented for Upstox yet - API not available");
                    return List.of();
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch SIP orders from {}: {}", brokerName, e.getMessage());
        }
        return List.of();
    }

    /**
     * Fetch orders from specific broker
     */
    private List<Map<String, Object>> fetchOrdersFromBroker(String brokerName, String userId) {
        try {
            Object rawOrders = null;
            switch (brokerName) {
                case "zerodha":
                    try {
                        rawOrders = zerodhaService.getOrders(userId);
                    } catch (IOException | com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException e) {
                        logger.warn("Failed to fetch Zerodha orders: {}", e.getMessage());
                        return List.of();
                    }
                    break;
                case "angelone":
                    rawOrders = angelOneService.getOrders(userId);
                    break;
                case "upstox":
                    rawOrders = upstoxService.getOrders(userId);
                    break;
            }

            if (rawOrders != null) {
                List<DataStandardizationService.StandardOrder> standardOrders = dataStandardizationService
                        .transformOrders(rawOrders, brokerName);
                // Convert to Map format for consistency
                List<Map<String, Object>> orderMaps = new java.util.ArrayList<>();
                for (DataStandardizationService.StandardOrder order : standardOrders) {
                    Map<String, Object> orderMap = new java.util.HashMap<>();
                    orderMap.put("order_id", order.orderId != null ? order.orderId : "");
                    orderMap.put("symbol", order.symbol != null ? order.symbol : "");
                    orderMap.put("exchange", order.exchange != null ? order.exchange : "");
                    orderMap.put("quantity", order.quantity != null ? order.quantity : 0);
                    orderMap.put("price", order.price != null ? order.price : 0.0);
                    orderMap.put("status", order.status != null ? order.status : "");
                    orderMap.put("order_type", order.orderType != null ? order.orderType : "");
                    orderMap.put("timestamp", order.timestamp != null ? order.timestamp : "");
                    orderMap.put("source", brokerName);
                    orderMaps.add(orderMap);
                }
                return orderMaps;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch orders from {}: {}", brokerName, e.getMessage());
        }
        return List.of();
    }

    /**
     * Calculate enhanced summary including MF/SIP/Orders
     */
    private Map<String, Object> calculateEnhancedSummary(
            List<DataStandardizationService.StandardHolding> holdings,
            List<DataStandardizationService.StandardPosition> positions,
            List<Map<String, Object>> mfHoldings,
            List<Map<String, Object>> sipOrders,
            List<Map<String, Object>> orders) {

        double holdingsValue = holdings.stream()
                .mapToDouble(h -> h.totalValue != null ? h.totalValue : 0.0)
                .sum();

        double holdingsPnL = holdings.stream()
                .mapToDouble(h -> h.pnl != null ? h.pnl : 0.0)
                .sum();

        double positionsPnL = positions.stream()
                .mapToDouble(p -> p.pnl != null ? p.pnl : 0.0)
                .sum();

        double mfValue = mfHoldings.stream()
                .mapToDouble(mf -> (Double) mf.getOrDefault("current_value", 0.0))
                .sum();

        double mfPnL = mfHoldings.stream()
                .mapToDouble(mf -> (Double) mf.getOrDefault("pnl", 0.0))
                .sum();

        double sipValue = sipOrders.stream()
                .mapToDouble(sip -> (Double) sip.getOrDefault("current_value", 0.0))
                .sum();

        Map<String, Object> summaryMap = new java.util.HashMap<>();
        summaryMap.put("total_value", holdingsValue + mfValue + sipValue);
        summaryMap.put("holdings_value", holdingsValue);
        summaryMap.put("mf_value", mfValue);
        summaryMap.put("sip_value", sipValue);
        summaryMap.put("total_pnl", holdingsPnL + positionsPnL + mfPnL);
        summaryMap.put("holdings_pnl", holdingsPnL);
        summaryMap.put("positions_pnl", positionsPnL);
        summaryMap.put("mf_pnl", mfPnL);
        summaryMap.put("holdings_count", holdings.size());
        summaryMap.put("positions_count", positions.size());
        summaryMap.put("mf_count", mfHoldings.size());
        summaryMap.put("sip_count", sipOrders.size());
        summaryMap.put("orders_count", orders.size());
        return summaryMap;
    }

    /**
     * Create empty enhanced summary
     */
    private Map<String, Object> createEmptyEnhancedSummary() {
        Map<String, Object> emptyMap = new java.util.HashMap<>();
        emptyMap.put("total_value", 0.0);
        emptyMap.put("total_pnl", 0.0);
        emptyMap.put("holdings_value", 0.0);
        emptyMap.put("holdings_pnl", 0.0);
        emptyMap.put("holdings_count", 0);
        emptyMap.put("positions_pnl", 0.0);
        emptyMap.put("positions_count", 0);
        emptyMap.put("mf_value", 0.0);
        emptyMap.put("mf_pnl", 0.0);
        emptyMap.put("mf_count", 0);
        emptyMap.put("sip_value", 0.0);
        emptyMap.put("sip_count", 0);
        emptyMap.put("orders_count", 0);
        return emptyMap;
    }

    /**
     * Aggregate enhanced portfolio data from multiple brokers
     */
    private Map<String, Object> aggregateEnhancedPortfolioData(
            Map<String, Object> zerodhaData, Map<String, Object> angelOneData, Map<String, Object> upstoxData,
            boolean includeHoldings, boolean includePositions, boolean includeMF,
            boolean includeSIP, boolean includeOrders) {

        Map<String, Object> aggregated = new java.util.HashMap<>();

        // Combine all data types
        if (includeHoldings) {
            List<DataStandardizationService.StandardHolding> allHoldings = new java.util.ArrayList<>();
            addHoldingsFromBrokerData(allHoldings, zerodhaData);
            addHoldingsFromBrokerData(allHoldings, angelOneData);
            addHoldingsFromBrokerData(allHoldings, upstoxData);
            aggregated.put("holdings", allHoldings);
        }

        if (includePositions) {
            List<DataStandardizationService.StandardPosition> allPositions = new java.util.ArrayList<>();
            addPositionsFromBrokerData(allPositions, zerodhaData);
            addPositionsFromBrokerData(allPositions, angelOneData);
            addPositionsFromBrokerData(allPositions, upstoxData);
            aggregated.put("positions", allPositions);
        }

        if (includeMF) {
            List<Map<String, Object>> allMFHoldings = new java.util.ArrayList<>();
            addMFHoldingsFromBrokerData(allMFHoldings, zerodhaData);
            addMFHoldingsFromBrokerData(allMFHoldings, angelOneData);
            addMFHoldingsFromBrokerData(allMFHoldings, upstoxData);
            aggregated.put("mf_holdings", allMFHoldings);
        }

        if (includeSIP) {
            List<Map<String, Object>> allSIPOrders = new java.util.ArrayList<>();
            addSIPOrdersFromBrokerData(allSIPOrders, zerodhaData);
            addSIPOrdersFromBrokerData(allSIPOrders, angelOneData);
            addSIPOrdersFromBrokerData(allSIPOrders, upstoxData);
            aggregated.put("sip_orders", allSIPOrders);
        }

        if (includeOrders) {
            List<Map<String, Object>> allOrders = new java.util.ArrayList<>();
            addOrdersFromBrokerData(allOrders, zerodhaData);
            addOrdersFromBrokerData(allOrders, angelOneData);
            addOrdersFromBrokerData(allOrders, upstoxData);
            aggregated.put("orders", allOrders);
        }

        // Calculate total summary
        Map<String, Object> totalSummary = calculateEnhancedTotalSummary(
                List.of(zerodhaData, angelOneData, upstoxData));
        aggregated.put("summary", totalSummary);

        return aggregated;
    }

    /**
     * Calculate enhanced total summary across all brokers
     */
    private Map<String, Object> calculateEnhancedTotalSummary(List<Map<String, Object>> brokerDataList) {
        double totalValue = 0.0, holdingsValue = 0.0, mfValue = 0.0, sipValue = 0.0;
        double totalPnL = 0.0, holdingsPnL = 0.0, positionsPnL = 0.0, mfPnL = 0.0;
        int holdingsCount = 0, positionsCount = 0, mfCount = 0, sipCount = 0, ordersCount = 0;

        for (Map<String, Object> brokerData : brokerDataList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> summary = (Map<String, Object>) brokerData.get("summary");
            if (summary != null) {
                totalValue += (Double) summary.getOrDefault("total_value", 0.0);
                holdingsValue += (Double) summary.getOrDefault("holdings_value", 0.0);
                mfValue += (Double) summary.getOrDefault("mf_value", 0.0);
                sipValue += (Double) summary.getOrDefault("sip_value", 0.0);
                totalPnL += (Double) summary.getOrDefault("total_pnl", 0.0);
                holdingsPnL += (Double) summary.getOrDefault("holdings_pnl", 0.0);
                positionsPnL += (Double) summary.getOrDefault("positions_pnl", 0.0);
                mfPnL += (Double) summary.getOrDefault("mf_pnl", 0.0);
                holdingsCount += (Integer) summary.getOrDefault("holdings_count", 0);
                positionsCount += (Integer) summary.getOrDefault("positions_count", 0);
                mfCount += (Integer) summary.getOrDefault("mf_count", 0);
                sipCount += (Integer) summary.getOrDefault("sip_count", 0);
                ordersCount += (Integer) summary.getOrDefault("orders_count", 0);
            }
        }

        Map<String, Object> totalMap = new java.util.HashMap<>();
        totalMap.put("total_value", totalValue);
        totalMap.put("holdings_value", holdingsValue);
        totalMap.put("mf_value", mfValue);
        totalMap.put("sip_value", sipValue);
        totalMap.put("total_pnl", totalPnL);
        totalMap.put("holdings_pnl", holdingsPnL);
        totalMap.put("positions_pnl", positionsPnL);
        totalMap.put("mf_pnl", mfPnL);
        totalMap.put("pnl_percentage", totalValue > 0 ? (totalPnL / (totalValue - totalPnL)) * 100 : 0.0);
        totalMap.put("holdings_count", holdingsCount);
        totalMap.put("positions_count", positionsCount);
        totalMap.put("mf_count", mfCount);
        totalMap.put("sip_count", sipCount);
        totalMap.put("orders_count", ordersCount);
        return totalMap;
    }

    // Helper methods to extract data from broker responses
    @SuppressWarnings("unchecked")
    private void addHoldingsFromBrokerData(List<DataStandardizationService.StandardHolding> allHoldings,
            Map<String, Object> brokerData) {
        List<DataStandardizationService.StandardHolding> holdings = (List<DataStandardizationService.StandardHolding>) brokerData
                .get("holdings");
        if (holdings != null) {
            allHoldings.addAll(holdings);
        }
    }

    @SuppressWarnings("unchecked")
    private void addPositionsFromBrokerData(List<DataStandardizationService.StandardPosition> allPositions,
            Map<String, Object> brokerData) {
        List<DataStandardizationService.StandardPosition> positions = (List<DataStandardizationService.StandardPosition>) brokerData
                .get("positions");
        if (positions != null) {
            allPositions.addAll(positions);
        }
    }

    @SuppressWarnings("unchecked")
    private void addMFHoldingsFromBrokerData(List<Map<String, Object>> allMFHoldings,
            Map<String, Object> brokerData) {
        List<Map<String, Object>> mfHoldings = (List<Map<String, Object>>) brokerData.get("mf_holdings");
        if (mfHoldings != null) {
            allMFHoldings.addAll(mfHoldings);
        }
    }

    @SuppressWarnings("unchecked")
    private void addSIPOrdersFromBrokerData(List<Map<String, Object>> allSIPOrders,
            Map<String, Object> brokerData) {
        List<Map<String, Object>> sipOrders = (List<Map<String, Object>>) brokerData.get("sip_orders");
        if (sipOrders != null) {
            allSIPOrders.addAll(sipOrders);
        }
    }

    @SuppressWarnings("unchecked")
    private void addOrdersFromBrokerData(List<Map<String, Object>> allOrders,
            Map<String, Object> brokerData) {
        List<Map<String, Object>> orders = (List<Map<String, Object>>) brokerData.get("orders");
        if (orders != null) {
            allOrders.addAll(orders);
        }
    }
}