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

import com.urva.myfinance.coinTrack.DTO.StandardizedDataResponse;
import com.urva.myfinance.coinTrack.Model.LiveMarketData;
import com.urva.myfinance.coinTrack.Service.BrokerService;
import com.urva.myfinance.coinTrack.Service.DataStandardizationService;
import com.urva.myfinance.coinTrack.Service.WebSocketService;

@RestController
@RequestMapping("/api/market")
@CrossOrigin(origins = "*")
public class MarketDataController {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private Map<String, BrokerService> brokerServices;

    @Autowired
    private DataStandardizationService dataStandardizationService;

    // Store SSE emitters for real-time streaming
    private final Set<SseEmitter> activeEmitters = ConcurrentHashMap.newKeySet();

    /**
     * Get latest market data for a symbol using StandardizedDataResponse
     * GET /api/market/live/{symbol}
     */
    @GetMapping("/live/{symbol}")
    public ResponseEntity<StandardizedDataResponse<?>> getLatestMarketData(
            @PathVariable String symbol,
            @RequestParam(required = false) String exchange) {

        try {
            LiveMarketData marketData = webSocketService.getLatestMarketData(symbol);

            if (marketData == null) {
                return ResponseEntity.notFound().build();
            }

            // Convert to standardized market data
            StandardizedDataResponse.MarketData standardizedData = new StandardizedDataResponse.MarketData();
            standardizedData.setSymbol(symbol);
            standardizedData.setExchange(exchange != null ? exchange : "NSE");
            standardizedData.setLtp(marketData.getLtp());
            standardizedData.setOpen(marketData.getOpen());
            standardizedData.setHigh(marketData.getHigh());
            standardizedData.setLow(marketData.getLow());
            standardizedData.setClose(marketData.getClose());
            standardizedData.setVolume(marketData.getVolume());
            standardizedData.setTimestamp(marketData.getTimestamp());
            standardizedData.setBrokerSource("websocket");

            StandardizedDataResponse<StandardizedDataResponse.MarketData> response = StandardizedDataResponse
                    .success(standardizedData, "market_data", "websocket");
            response.setTimestamp(LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching market data for symbol: {}", symbol, e);
            StandardizedDataResponse<?> errorResponse = StandardizedDataResponse
                    .error("Failed to fetch market data: " + e.getMessage(), "websocket");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get market data for multiple symbols using StandardizedDataResponse
     * GET /api/market/live/batch?symbols=RELIANCE,TCS,INFY
     */
    @GetMapping("/live/batch")
    public ResponseEntity<StandardizedDataResponse<?>> getBatchMarketData(
            @RequestParam String symbols,
            @RequestParam(required = false) String exchange) {

        try {
            String[] symbolList = symbols.split(",");
            List<StandardizedDataResponse.MarketData> marketDataList = new java.util.ArrayList<>();

            for (String symbol : symbolList) {
                LiveMarketData data = webSocketService.getLatestMarketData(symbol.trim());
                if (data != null) {
                    StandardizedDataResponse.MarketData standardizedData = new StandardizedDataResponse.MarketData();
                    standardizedData.setSymbol(symbol.trim());
                    standardizedData.setExchange(exchange != null ? exchange : "NSE");
                    standardizedData.setLtp(data.getLtp());
                    standardizedData.setOpen(data.getOpen());
                    standardizedData.setHigh(data.getHigh());
                    standardizedData.setLow(data.getLow());
                    standardizedData.setClose(data.getClose());
                    standardizedData.setVolume(data.getVolume());
                    standardizedData.setTimestamp(data.getTimestamp());
                    standardizedData.setBrokerSource("websocket");
                    marketDataList.add(standardizedData);
                }
            }

            StandardizedDataResponse<List<StandardizedDataResponse.MarketData>> response = StandardizedDataResponse
                    .success(marketDataList, "market_data", "websocket");
            response.setTimestamp(LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching batch market data", e);
            StandardizedDataResponse<?> errorResponse = StandardizedDataResponse
                    .error("Failed to fetch batch market data: " + e.getMessage(), "websocket");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Stream real-time market data using Server-Sent Events
     * GET /api/market/live/stream?symbols=RELIANCE,TCS
     */
    @GetMapping(value = "/live/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMarketData(
            @RequestParam(required = false) String symbols,
            @RequestParam(required = false) String exchange) {

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        activeEmitters.add(emitter);

        emitter.onCompletion(() -> activeEmitters.remove(emitter));
        emitter.onTimeout(() -> activeEmitters.remove(emitter));
        emitter.onError((e) -> activeEmitters.remove(emitter));

        try {
            // Send initial connection message
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "message", "Connected to market data stream",
                            "symbols", symbols != null ? symbols : "all",
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))));

        } catch (Exception e) {
            logger.error("Error starting market data stream", e);
            activeEmitters.remove(emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Subscribe to additional symbols during an active stream
     * POST /api/market/live/subscribe
     */
    @PostMapping("/live/subscribe")
    public ResponseEntity<StandardizedDataResponse<?>> subscribeToSymbols(@RequestBody Map<String, Object> request) {
        try {
            String symbols = (String) request.get("symbols");

            if (symbols == null || symbols.isEmpty()) {
                StandardizedDataResponse<?> errorResponse = StandardizedDataResponse
                        .error("Symbols parameter is required", "websocket");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String[] symbolList = symbols.split(",");

            // Subscribe to symbols in WebSocket service
            for (String symbol : symbolList) {
                webSocketService.subscribe(symbol.trim());
            }

            Map<String, Object> result = Map.of(
                    "message", "Subscription request received",
                    "symbols_requested", symbolList.length,
                    "active_connections", activeEmitters.size());

            StandardizedDataResponse<Map<String, Object>> response = StandardizedDataResponse.success(result,
                    "subscription", "websocket");
            response.setTimestamp(LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error subscribing to symbols", e);
            StandardizedDataResponse<?> errorResponse = StandardizedDataResponse
                    .error("Failed to subscribe to symbols: " + e.getMessage(), "websocket");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get current WebSocket connection status
     * GET /api/market/live/status
     */
    @GetMapping("/live/status")
    public ResponseEntity<StandardizedDataResponse<?>> getWebSocketStatus() {
        try {
            Map<String, Object> status = webSocketService.getConnectionStatus();
            status.put("active_sse_connections", activeEmitters.size());
            status.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            StandardizedDataResponse<Map<String, Object>> response = StandardizedDataResponse.success(status, "status",
                    "websocket");
            response.setTimestamp(LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting WebSocket status", e);
            StandardizedDataResponse<?> errorResponse = StandardizedDataResponse
                    .error("Failed to get status: " + e.getMessage(), "websocket");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get historical candle data for a symbol
     * GET
     * /api/market/history/{symbol}?interval=5m&from=2025-01-01&to=2025-01-31&exchange=NSE&source=angelone
     */
    @GetMapping("/history/{symbol}")
    public ResponseEntity<StandardizedDataResponse<?>> getHistoricalData(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam(defaultValue = "angelone") String source) {

        try {
            logger.info(
                    "Historical data request for symbol: {}, interval: {}, from: {}, to: {}, exchange: {}, source: {}",
                    symbol, interval, from, to, exchange, source);

            BrokerService brokerService = brokerServices.get(source.toLowerCase());
            if (brokerService == null) {
                StandardizedDataResponse<?> errorResponse = StandardizedDataResponse
                        .error("Unsupported broker: " + source, source);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Fetch historical data from broker
            Map<String, Object> historicalData = brokerService.getHistoricalData(
                    "defaultUser", symbol, from, to, interval);

            // Convert to standardized format
            StandardizedDataResponse.HistoricalData standardizedData = new StandardizedDataResponse.HistoricalData();
            standardizedData.setSymbol(symbol);
            standardizedData.setExchange(exchange);
            standardizedData.setInterval(interval);
            standardizedData.setBrokerSource(source);
            standardizedData.setFromDate(java.time.LocalDate.parse(from).atStartOfDay());
            standardizedData.setToDate(java.time.LocalDate.parse(to).atStartOfDay());

            // Convert candles from historical data
            List<StandardizedDataResponse.Candle> candles = convertHistoricalDataToCandles(historicalData);
            standardizedData.setCandles(candles);

            StandardizedDataResponse<StandardizedDataResponse.HistoricalData> response = StandardizedDataResponse
                    .success(standardizedData, "historical_data", source);
            response.setTimestamp(LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching historical data for symbol: {}", symbol, e);
            StandardizedDataResponse<?> errorResponse = StandardizedDataResponse
                    .error("Failed to fetch historical data: " + e.getMessage(), source);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get available intervals for historical data
     * GET /api/market/history/intervals
     */
    @GetMapping("/history/intervals")
    public ResponseEntity<StandardizedDataResponse<?>> getAvailableIntervals() {
        try {
            List<Map<String, Object>> intervals = List.of(
                    Map.of("value", "1m", "label", "1 Minute", "description", "1-minute candles"),
                    Map.of("value", "5m", "label", "5 Minutes", "description", "5-minute candles"),
                    Map.of("value", "15m", "label", "15 Minutes", "description", "15-minute candles"),
                    Map.of("value", "30m", "label", "30 Minutes", "description", "30-minute candles"),
                    Map.of("value", "1h", "label", "1 Hour", "description", "1-hour candles"),
                    Map.of("value", "1d", "label", "1 Day", "description", "Daily candles"),
                    Map.of("value", "1w", "label", "1 Week", "description", "Weekly candles"),
                    Map.of("value", "1M", "label", "1 Month", "description", "Monthly candles"));

            StandardizedDataResponse<List<Map<String, Object>>> response = StandardizedDataResponse.success(intervals,
                    "intervals", "system");
            response.setTimestamp(LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting available intervals", e);
            StandardizedDataResponse<?> errorResponse = StandardizedDataResponse
                    .error("Failed to get intervals: " + e.getMessage(), "system");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get supported exchanges
     * GET /api/market/exchanges
     */
    @GetMapping("/exchanges")
    public ResponseEntity<StandardizedDataResponse<?>> getSupportedExchanges() {
        try {
            List<Map<String, Object>> exchanges = List.of(
                    Map.of("value", "NSE", "label", "National Stock Exchange", "country", "India"),
                    Map.of("value", "BSE", "label", "Bombay Stock Exchange", "country", "India"),
                    Map.of("value", "MCX", "label", "Multi Commodity Exchange", "country", "India"),
                    Map.of("value", "NCDEX", "label", "National Commodity & Derivatives Exchange", "country", "India"));

            StandardizedDataResponse<List<Map<String, Object>>> response = StandardizedDataResponse.success(exchanges,
                    "exchanges", "system");
            response.setTimestamp(LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting supported exchanges", e);
            StandardizedDataResponse<?> errorResponse = StandardizedDataResponse
                    .error("Failed to get exchanges: " + e.getMessage(), "system");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Broadcast market data update to all SSE clients
     * This method would be called by WebSocketService when new data arrives
     */
    public void broadcastMarketData(LiveMarketData marketData) {
        if (activeEmitters.isEmpty()) {
            return;
        }

        Set<SseEmitter> deadEmitters = ConcurrentHashMap.newKeySet();

        for (SseEmitter emitter : activeEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("market-data")
                        .data(marketData));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        // Remove dead connections
        activeEmitters.removeAll(deadEmitters);
    }

    /**
     * Helper method to convert historical data from broker format to standardized candles
     */
    private List<StandardizedDataResponse.Candle> convertHistoricalDataToCandles(Map<String, Object> historicalData) {
        List<StandardizedDataResponse.Candle> candles = new java.util.ArrayList<>();

        try {
            // This is a placeholder implementation
            // In a real implementation, you would parse the broker-specific response format
            // and convert it to StandardizedDataResponse.Candle objects

            if (historicalData != null && historicalData.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) historicalData.get("data");

                if (dataList != null) {
                    for (@SuppressWarnings("unused") Map<String, Object> candleData : dataList) {
                        StandardizedDataResponse.Candle candle = new StandardizedDataResponse.Candle();
                        // Map broker-specific fields to standardized candle fields
                        // This would need to be customized based on each broker's response format
                        candle.setTimestamp(LocalDateTime.now()); // Placeholder
                        candle.setOpen(0.0); // Placeholder
                        candle.setHigh(0.0); // Placeholder
                        candle.setLow(0.0); // Placeholder
                        candle.setClose(0.0); // Placeholder
                        candle.setVolume(0L); // Placeholder
                        candles.add(candle);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error converting historical data to candles", e);
        }

        return candles;
    }
}