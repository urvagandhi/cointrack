package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.urva.myfinance.coinTrack.Model.LiveMarketData;
import com.urva.myfinance.coinTrack.Service.WebSocketManager;

@RestController
@RequestMapping("/api/live")
@CrossOrigin(origins = "*")
public class LiveMarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(LiveMarketDataController.class);

    @Autowired
    private WebSocketManager webSocketManager;

    // Store SSE emitters for real-time streaming
    private final Set<SseEmitter> activeEmitters = ConcurrentHashMap.newKeySet();

    /**
     * Get latest market data for a symbol
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<?> getLatestMarketData(@PathVariable String symbol,
            @RequestParam(required = false) String exchange) {
        try {
            LiveMarketData marketData;

            if (exchange != null && !exchange.isEmpty()) {
                marketData = webSocketManager.getLatestMarketData(symbol, exchange);
            } else {
                marketData = webSocketManager.getLatestMarketData(symbol);
            }

            if (marketData != null) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "data", marketData,
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                return ResponseEntity.ok(Map.of(
                        "status", "not_found",
                        "message", "No market data found for symbol: " + symbol,
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

        } catch (Exception e) {
            logger.error("Error fetching market data for symbol: {}", symbol, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch market data: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get latest market data for multiple symbols
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> getBulkMarketData(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> symbols = (List<String>) request.get("symbols");

            if (symbols == null || symbols.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Symbols list is required",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            List<LiveMarketData> marketDataList = webSocketManager.getLatestMarketDataForSymbols(symbols);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", marketDataList,
                    "count", marketDataList.size(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error fetching bulk market data", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch bulk market data: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get market data history for a symbol
     */
    @GetMapping("/{symbol}/history")
    public ResponseEntity<?> getMarketDataHistory(@PathVariable String symbol,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");

            List<LiveMarketData> history = webSocketManager.getMarketDataHistory(symbol, start, end);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", history,
                    "count", history.size(),
                    "symbol", symbol,
                    "startDate", startDate,
                    "endDate", endDate,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error fetching market data history for symbol: {}", symbol, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch market data history: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Subscribe to a symbol
     */
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribeToSymbol(@RequestBody Map<String, String> request) {
        try {
            String symbol = request.get("symbol");

            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Symbol is required",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            webSocketManager.subscribe(symbol.trim().toUpperCase());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Subscribed to symbol: " + symbol.toUpperCase(),
                    "symbol", symbol.toUpperCase(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error subscribing to symbol", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to subscribe: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Unsubscribe from a symbol
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeFromSymbol(@RequestBody Map<String, String> request) {
        try {
            String symbol = request.get("symbol");

            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Symbol is required",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            webSocketManager.unsubscribe(symbol.trim().toUpperCase());

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Unsubscribed from symbol: " + symbol.toUpperCase(),
                    "symbol", symbol.toUpperCase(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error unsubscribing from symbol", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to unsubscribe: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get WebSocket Manager status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getConnectionStatus() {
        try {
            Map<String, Object> status = webSocketManager.getConnectionStatus();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", status,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error getting connection status", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to get connection status: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get market statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getMarketStatistics() {
        try {
            Map<String, Object> statistics = webSocketManager.getMarketStatistics();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", statistics,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error getting market statistics", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to get market statistics: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get subscribed symbols
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<?> getSubscribedSymbols() {
        try {
            Set<String> symbols = webSocketManager.getSubscribedSymbols();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", symbols,
                    "count", symbols.size(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error getting subscribed symbols", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to get subscribed symbols: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Server-Sent Events endpoint for real-time market data streaming
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMarketData(@RequestParam(required = false) String symbols) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        try {
            activeEmitters.add(emitter);

            // Send initial connection message
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "message", "Connected to live market data stream",
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            "connectionId", emitter.hashCode())));

            // Handle client disconnect
            emitter.onCompletion(() -> {
                activeEmitters.remove(emitter);
                logger.debug("SSE client disconnected. Active connections: {}", activeEmitters.size());
            });

            emitter.onTimeout(() -> {
                activeEmitters.remove(emitter);
                logger.debug("SSE connection timed out. Active connections: {}", activeEmitters.size());
            });

            emitter.onError((e) -> {
                activeEmitters.remove(emitter);
                logger.error("SSE connection error", e);
            });

            logger.debug("New SSE client connected. Active connections: {}", activeEmitters.size());

        } catch (Exception e) {
            logger.error("Error setting up SSE connection", e);
            activeEmitters.remove(emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Force reconnection of WebSocket connectors
     */
    @PostMapping("/reconnect")
    public ResponseEntity<?> forceReconnect() {
        try {
            webSocketManager.forceReconnect();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Reconnection initiated",
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error forcing reconnection", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to force reconnection: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Broadcast market data update to all SSE clients
     * This method would be called by WebSocketManager when new data arrives
     */
    public void broadcastMarketData(LiveMarketData marketData) {
        if (activeEmitters.isEmpty()) {
            return;
        }

        Set<SseEmitter> deadEmitters = ConcurrentHashMap.newKeySet();

        for (SseEmitter emitter : activeEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("marketData")
                        .data(Map.of(
                                "symbol", marketData.getSymbol(),
                                "exchange", marketData.getExchange(),
                                "ltp", marketData.getLtp(),
                                "change", marketData.getChange(),
                                "changePercent", marketData.getChangePercent(),
                                "volume", marketData.getVolume(),
                                "timestamp", marketData.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));
            } catch (Exception e) {
                logger.debug("Failed to send market data to SSE client, removing dead connection", e);
                deadEmitters.add(emitter);
            }
        }

        // Remove dead connections
        activeEmitters.removeAll(deadEmitters);
    }
}