package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.Service.AngelOneService;
import com.urva.myfinance.coinTrack.Service.DataStandardizationService;
import com.urva.myfinance.coinTrack.Service.UpstoxService;

@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class HistoricalDataController {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalDataController.class);

    @Autowired
    private AngelOneService angelOneService;

    @Autowired
    private UpstoxService upstoxService;

    @Autowired
    private DataStandardizationService dataStandardizationService;

    /**
     * Get historical candle data for a symbol
     * /api/history/RELIANCE?interval=5m&from=2025-09-01&to=2025-09-19&exchange=NSE&source=nse
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<?> getHistoricalData(@PathVariable String symbol,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "NSE") String exchange,
            @RequestParam(defaultValue = "auto") String source) {
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);

            // Validate date range
            if (fromDate.isAfter(toDate)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "From date cannot be after to date",
                        "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            // Auto-select source if not specified
            String selectedSource = source;
            if ("auto".equals(source)) {
                selectedSource = "zerodha"; // Default to Zerodha for historical data
            }

            Object historicalData = null;
            String actualSource = selectedSource;

            // Fetch historical data based on source
            switch (selectedSource.toLowerCase()) {
                case "zerodha":
                    try {
                        // Zerodha doesn't have historical data method implemented, skip for now
                        logger.warn("Zerodha historical data not available, trying Angel One");
                        historicalData = angelOneService.getHistoricalData("defaultUser", exchange, symbol,
                                interval, fromDate.toString(), toDate.toString());
                        actualSource = "angelone";
                    } catch (Exception e) {
                        logger.warn("Failed to fetch from Angel One, trying Upstox", e);
                        // Fallback to Upstox
                        try {
                            historicalData = upstoxService.getHistoricalData("defaultUser", symbol, interval,
                                    fromDate.toString(), toDate.toString());
                            actualSource = "upstox";
                        } catch (Exception e2) {
                            logger.warn("Failed to fetch from Upstox, using mock data", e2);
                            historicalData = generateMockHistoricalData(symbol, exchange, interval, fromDate, toDate);
                            actualSource = "mock";
                        }
                    }
                    break;

                case "angelone":
                    historicalData = angelOneService.getHistoricalData("defaultUser", exchange, symbol,
                            interval, fromDate.toString(), toDate.toString());
                    break;

                case "upstox":
                    historicalData = upstoxService.getHistoricalData("defaultUser", symbol, interval,
                            fromDate.toString(), toDate.toString());
                    break;

                case "nse":
                case "bse":
                    // Mock NSE/BSE historical data for now
                    historicalData = generateMockHistoricalData(symbol, exchange, interval, fromDate, toDate);
                    actualSource = selectedSource;
                    break;

                default:
                    return ResponseEntity.badRequest().body(Map.of(
                            "status", "error",
                            "message", "Invalid source. Supported: zerodha, angelone, upstox, nse, bse",
                            "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            }

            // Standardize the response format
            DataStandardizationService.StandardHistoricalData standardizedData = dataStandardizationService
                    .transformHistoricalData(historicalData, symbol, exchange, interval, actualSource);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "symbol", standardizedData.symbol,
                            "exchange", standardizedData.exchange,
                            "interval", standardizedData.interval,
                            "from", from,
                            "to", to,
                            "source", standardizedData.source,
                            "candles", standardizedData.candles,
                            "count", standardizedData.candles.size()),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error fetching historical data for symbol: {}", symbol, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to fetch historical data: " + e.getMessage(),
                    "symbol", symbol,
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Get supported intervals for historical data
     */
    @GetMapping("/intervals")
    public ResponseEntity<?> getSupportedIntervals() {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "intervals", List.of("1m", "3m", "5m", "15m", "30m", "1h", "1d"),
                            "sources", List.of("zerodha", "angelone", "upstox", "nse", "bse"),
                            "exchanges", List.of("NSE", "BSE", "MCX", "NFO"),
                            "maxDays", Map.of(
                                    "1m", 30,
                                    "5m", 60,
                                    "1h", 365,
                                    "1d", 3650)),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

        } catch (Exception e) {
            logger.error("Error getting supported intervals", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to get supported intervals: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }
    }

    /**
     * Generate mock historical data for NSE/BSE (until real integration)
     */
    private Object generateMockHistoricalData(String symbol, String exchange, String interval,
            LocalDate fromDate, LocalDate toDate) {
        List<Map<String, Object>> mockCandles = new ArrayList<>();

        // Generate mock candle data
        LocalDate currentDate = fromDate;
        double basePrice = 1000.0; // Mock base price

        while (!currentDate.isAfter(toDate)) {
            // Generate mock OHLC data
            double open = basePrice + (Math.random() - 0.5) * 50;
            double high = open + Math.random() * 20;
            double low = open - Math.random() * 20;
            double close = low + Math.random() * (high - low);
            long volume = (long) (10000 + Math.random() * 50000);

            Map<String, Object> candle = Map.of(
                    "time", currentDate.atTime(9, 15).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "open", Math.round(open * 100.0) / 100.0,
                    "high", Math.round(high * 100.0) / 100.0,
                    "low", Math.round(low * 100.0) / 100.0,
                    "close", Math.round(close * 100.0) / 100.0,
                    "volume", volume);

            mockCandles.add(candle);
            currentDate = currentDate.plusDays(1);
            basePrice = close; // Use previous close as next base
        }

        return Map.of(
                "symbol", symbol,
                "exchange", exchange,
                "interval", interval,
                "candles", mockCandles);
    }
}