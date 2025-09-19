package com.urva.myfinance.coinTrack.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.urva.myfinance.coinTrack.Model.LiveMarketData;
import com.urva.myfinance.coinTrack.Repository.LiveMarketDataRepository;
import com.urva.myfinance.coinTrack.Service.Connector.BSEWebSocketConnector;
import com.urva.myfinance.coinTrack.Service.Connector.NSEWebSocketConnector;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class WebSocketManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);

    @Autowired
    private NSEWebSocketConnector nseConnector;

    @Autowired
    private BSEWebSocketConnector bseConnector;

    @Autowired
    private LiveMarketDataRepository liveMarketDataRepository;

    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(4);
    private volatile boolean isRunning = false;

    /**
     * Initialize the WebSocket Manager
     */
    @PostConstruct
    public void initialize() {
        try {
            logger.info("Initializing WebSocket Manager...");

            // Initialize connectors
            nseConnector.initialize();
            bseConnector.initialize();

            // Start cleanup task
            scheduleDataCleanup();

            // Start health monitoring
            scheduleHealthMonitoring();

            // Start periodic refresh/resubscribe with jitter between 15-30 seconds
            schedulePeriodicRefresh();

            isRunning = true;
            logger.info("WebSocket Manager initialized successfully");

            // Subscribe to some default symbols
            subscribeToDefaultSymbols();

        } catch (Exception e) {
            logger.error("Failed to initialize WebSocket Manager", e);
        }
    }

    /**
     * Schedule a periodic refresh that runs with jitter between 15 and 30 seconds.
     * This helps keep subscriptions alive and can re-affirm symbol subscriptions
     * in case of transient disconnects. Uses the existing scheduler.
     */
    private void schedulePeriodicRefresh() {
        Runnable refreshTask = () -> {
            try {
                if (!isRunning) return;

                // small jitter between 15 and 30 seconds
                int jitterSeconds = ThreadLocalRandom.current().nextInt(15, 31);

                logger.debug("Periodic subscription refresh starting (jitter {}s)", jitterSeconds);

                // Re-affirm subscriptions by re-subscribing to all symbols (connector should dedupe)
                for (String symbol : subscribedSymbols) {
                    try {
                        nseConnector.subscribe(symbol);
                        bseConnector.subscribe(symbol);
                    } catch (Exception ex) {
                        logger.debug("Failed to refresh subscription for {}: {}", symbol, ex.getMessage());
                    }
                }

            } catch (Exception e) {
                logger.error("Periodic refresh failed", e);
            }
        };

        // Schedule at fixed rate with initial delay chosen randomly in the same window
        int initialDelay = ThreadLocalRandom.current().nextInt(1, 6);
        scheduler.scheduleAtFixedRate(refreshTask, initialDelay, 20, TimeUnit.SECONDS);
        logger.info("Scheduled periodic subscription refresh task (intended jitter 15-30s window)");
    }

    /**
     * Subscribe to a symbol on both NSE and BSE
     */
    public void subscribe(String symbol) {
        if (subscribedSymbols.add(symbol.toUpperCase())) {
            try {
                nseConnector.subscribe(symbol.toUpperCase());
                bseConnector.subscribe(symbol.toUpperCase());
                logger.info("Subscribed to symbol: {} on both NSE and BSE", symbol);
            } catch (Exception e) {
                logger.error("Failed to subscribe to symbol: {}", symbol, e);
                subscribedSymbols.remove(symbol.toUpperCase());
            }
        }
    }

    /**
     * Subscribe to multiple symbols
     */
    public void subscribeToSymbols(List<String> symbols) {
        for (String symbol : symbols) {
            subscribe(symbol);
        }
    }

    /**
     * Unsubscribe from a symbol
     */
    public void unsubscribe(String symbol) {
        if (subscribedSymbols.remove(symbol.toUpperCase())) {
            try {
                nseConnector.unsubscribe(symbol.toUpperCase());
                bseConnector.unsubscribe(symbol.toUpperCase());
                logger.info("Unsubscribed from symbol: {} on both NSE and BSE", symbol);
            } catch (Exception e) {
                logger.error("Failed to unsubscribe from symbol: {}", symbol, e);
            }
        }
    }

    /**
     * Get latest market data for a symbol
     */
    public LiveMarketData getLatestMarketData(String symbol) {
        return liveMarketDataRepository.findTopBySymbolOrderByTimestampDesc(symbol.toUpperCase())
                .orElse(null);
    }

    /**
     * Get latest market data for a symbol from specific exchange
     */
    public LiveMarketData getLatestMarketData(String symbol, String exchange) {
        return liveMarketDataRepository.findTopBySymbolAndExchangeOrderByTimestampDesc(
                symbol.toUpperCase(), exchange.toUpperCase()).orElse(null);
    }

    /**
     * Get latest market data for multiple symbols
     */
    public List<LiveMarketData> getLatestMarketDataForSymbols(List<String> symbols) {
        return liveMarketDataRepository.findLatestBySymbols(
                symbols.stream().map(String::toUpperCase).toList());
    }

    /**
     * Get market data for a symbol within time range
     */
    public List<LiveMarketData> getMarketDataHistory(String symbol, LocalDateTime start, LocalDateTime end) {
        return liveMarketDataRepository.findBySymbolAndTimestampBetweenOrderByTimestampDesc(
                symbol.toUpperCase(), start, end);
    }

    /**
     * Get connection status
     */
    public Map<String, Object> getConnectionStatus() {
        return Map.of(
                "isRunning", isRunning,
                "nseConnected", nseConnector.isConnected(),
                "bseConnected", bseConnector.isConnected(),
                "subscribedSymbols", subscribedSymbols.size(),
                "nseSubscriptions", nseConnector.getSubscribedSymbolsCount(),
                "bseSubscriptions", bseConnector.getSubscribedSymbolsCount(),
                "totalMarketDataRecords", liveMarketDataRepository.count(),
                "lastUpdated", LocalDateTime.now());
    }

    /**
     * Get subscribed symbols
     */
    public Set<String> getSubscribedSymbols() {
        return Set.copyOf(subscribedSymbols);
    }

    /**
     * Subscribe to default symbols for testing
     */
    private void subscribeToDefaultSymbols() {
        List<String> defaultSymbols = List.of(
                "RELIANCE", "TCS", "INFY", "HDFCBANK", "ICICIBANK",
                "HINDUNILVR", "SBIN", "BHARTIARTL", "ITC", "KOTAKBANK");

        scheduler.schedule(() -> {
            logger.info("Subscribing to default symbols...");
            subscribeToSymbols(defaultSymbols);
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * Schedule data cleanup to remove old records
     */
    private void scheduleDataCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Delete market data older than 7 days
                LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
                liveMarketDataRepository.deleteByTimestampBefore(cutoff);
                logger.debug("Cleaned up old market data records before: {}", cutoff);
            } catch (Exception e) {
                logger.error("Failed to clean up old market data", e);
            }
        }, 1, 24, TimeUnit.HOURS); // Run daily
    }

    /**
     * Schedule health monitoring
     */
    private void scheduleHealthMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> status = getConnectionStatus();
                logger.info("WebSocket Manager Health: NSE={}, BSE={}, Symbols={}, Records={}",
                        status.get("nseConnected"),
                        status.get("bseConnected"),
                        status.get("subscribedSymbols"),
                        status.get("totalMarketDataRecords"));

                // Check for stale data
                List<LiveMarketData> recentData = liveMarketDataRepository.findActiveSymbols(
                        LocalDateTime.now().minusMinutes(1));

                if (recentData.isEmpty() && !subscribedSymbols.isEmpty()) {
                    logger.warn("No recent market data received, connections may be stale");
                }

            } catch (Exception e) {
                logger.error("Health monitoring failed", e);
            }
        }, 1, 1, TimeUnit.MINUTES); // Check every 11 minutes
    }

    /**
     * Get market statistics
     */
    public Map<String, Object> getMarketStatistics() {
        try {
            long totalRecords = liveMarketDataRepository.count();

            // Get records from last hour
            LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
            List<LiveMarketData> recentData = liveMarketDataRepository.findActiveSymbols(lastHour);

            // Get unique symbols and exchanges
            Set<String> activeSymbols = ConcurrentHashMap.newKeySet();
            Set<String> activeExchanges = ConcurrentHashMap.newKeySet();

            for (LiveMarketData data : recentData) {
                activeSymbols.add(data.getSymbol());
                activeExchanges.add(data.getExchange());
            }

            return Map.of(
                    "totalRecords", totalRecords,
                    "recentRecords", recentData.size(),
                    "activeSymbols", activeSymbols.size(),
                    "activeExchanges", activeExchanges.size(),
                    "subscribedSymbols", subscribedSymbols.size(),
                    "connectionStatus", getConnectionStatus());

        } catch (Exception e) {
            logger.error("Failed to get market statistics", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Force reconnection of all connectors
     */
    public void forceReconnect() {
        logger.info("Forcing reconnection of all WebSocket connectors...");

        scheduler.execute(() -> {
            try {
                // Shutdown existing connections
                nseConnector.shutdown();
                bseConnector.shutdown();

                // Wait a bit
                Thread.sleep(2000);

                // Reinitialize
                nseConnector.initialize();
                bseConnector.initialize();

                // Resubscribe to all symbols
                scheduler.schedule(() -> {
                    for (String symbol : subscribedSymbols) {
                        nseConnector.subscribe(symbol);
                        bseConnector.subscribe(symbol);
                    }
                }, 3, TimeUnit.SECONDS);

                logger.info("WebSocket connectors reconnected successfully");

            } catch (Exception e) {
                logger.error("Failed to reconnect WebSocket connectors", e);
            }
        });
    }

    /**
     * Shutdown the WebSocket Manager
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down WebSocket Manager...");

        isRunning = false;

        try {
            nseConnector.shutdown();
            bseConnector.shutdown();
            scheduler.shutdown();

            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }

            logger.info("WebSocket Manager shutdown completed");

        } catch (Exception e) {
            logger.error("Error during WebSocket Manager shutdown", e);
            scheduler.shutdownNow();
        }
    }
}