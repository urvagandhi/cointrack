package com.urva.myfinance.coinTrack.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DataStandardizationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Standardized market data format
     */
    public static class StandardMarketData {
        public String symbol;
        public String exchange;
        public Double ltp;
        public Double bid;
        public Double ask;
        public Long volume;
        public Double open;
        public Double high;
        public Double low;
        public Double close;
        public String timestamp;
        public String source;
    }

    /**
     * Standardized candle data format
     */
    public static class StandardCandle {
        public String time;
        public Double open;
        public Double high;
        public Double low;
        public Double close;
        public Long volume;
    }

    /**
     * Standardized historical data format
     */
    public static class StandardHistoricalData {
        public String symbol;
        public String exchange;
        public String interval;
        public List<StandardCandle> candles;
        public String source;
    }

    /**
     * Standardized holding data format
     */
    public static class StandardHolding {
        public String symbol;
        public String exchange;
        public String tradingsymbol;
        public Long quantity;
        public Double averagePrice;
        public Double currentPrice;
        public Double totalValue;
        public Double pnl;
        public Double pnlPercentage;
        public String source;
    }

    /**
     * Standardized position data format
     */
    public static class StandardPosition {
        public String symbol;
        public String exchange;
        public String tradingsymbol;
        public String product; // MIS, CNC, NRML
        public Long quantity;
        public Double averagePrice;
        public Double ltp;
        public Double pnl;
        public Double pnlPercentage;
        public String source;
    }

    /**
     * Standardized order data format
     */
    public static class StandardOrder {
        public String orderId;
        public String symbol;
        public String exchange;
        public String tradingsymbol;
        public String orderType; // BUY, SELL
        public String product; // MIS, CNC, NRML
        public Long quantity;
        public Double price;
        public String status; // OPEN, COMPLETE, CANCELLED, REJECTED
        public String timestamp;
        public String source;
    }

    // ============ ZERODHA DATA TRANSFORMATION ============

    /**
     * Transform Zerodha market data to standard format
     */
    @SuppressWarnings("unchecked")
    public StandardMarketData transformZerodhaMarketData(Object zerodhaData, String symbol) {
        try {
            Map<String, Object> data = (Map<String, Object>) zerodhaData;
            StandardMarketData standardData = new StandardMarketData();

            standardData.symbol = symbol;
            standardData.exchange = "NSE"; // Default, should be extracted from data
            standardData.ltp = getDoubleValue(data, "last_price");
            standardData.bid = getDoubleValue(data, "buy_quantity");
            standardData.ask = getDoubleValue(data, "sell_quantity");
            standardData.volume = getLongValue(data, "volume");
            standardData.open = getDoubleValue(data, "ohlc.open");
            standardData.high = getDoubleValue(data, "ohlc.high");
            standardData.low = getDoubleValue(data, "ohlc.low");
            standardData.close = getDoubleValue(data, "ohlc.close");
            standardData.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            standardData.source = "zerodha";

            return standardData;
        } catch (Exception e) {
            throw new RuntimeException("Error transforming Zerodha market data", e);
        }
    }

    /**
     * Transform Zerodha holdings to standard format
     */
    @SuppressWarnings("unchecked")
    public List<StandardHolding> transformZerodhaHoldings(Object zerodhaData) {
        try {
            List<StandardHolding> standardHoldings = new ArrayList<>();

            if (zerodhaData instanceof Map) {
                Map<String, Object> response = (Map<String, Object>) zerodhaData;
                List<Map<String, Object>> holdings = (List<Map<String, Object>>) response.get("data");

                if (holdings != null) {
                    for (Map<String, Object> holding : holdings) {
                        StandardHolding standardHolding = new StandardHolding();

                        standardHolding.symbol = getStringValue(holding, "instrument_token");
                        standardHolding.exchange = getStringValue(holding, "exchange");
                        standardHolding.tradingsymbol = getStringValue(holding, "tradingsymbol");
                        standardHolding.quantity = getLongValue(holding, "quantity");
                        standardHolding.averagePrice = getDoubleValue(holding, "average_price");
                        standardHolding.currentPrice = getDoubleValue(holding, "last_price");
                        standardHolding.totalValue = getDoubleValue(holding, "value");
                        standardHolding.pnl = getDoubleValue(holding, "pnl");
                        standardHolding.pnlPercentage = calculatePnlPercentage(
                                standardHolding.averagePrice, standardHolding.currentPrice);
                        standardHolding.source = "zerodha";

                        standardHoldings.add(standardHolding);
                    }
                }
            }

            return standardHoldings;
        } catch (Exception e) {
            throw new RuntimeException("Error transforming Zerodha holdings", e);
        }
    }

    // ============ ANGEL ONE DATA TRANSFORMATION ============

    /**
     * Transform Angel One market data to standard format
     */
    @SuppressWarnings("unchecked")
    public StandardMarketData transformAngelOneMarketData(Object angelData, String symbol) {
        try {
            Map<String, Object> response = (Map<String, Object>) angelData;
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            StandardMarketData standardData = new StandardMarketData();

            standardData.symbol = symbol;
            standardData.exchange = getStringValue(data, "exchange");
            standardData.ltp = getDoubleValue(data, "ltp");
            standardData.bid = getDoubleValue(data, "best_bid_price");
            standardData.ask = getDoubleValue(data, "best_ask_price");
            standardData.volume = getLongValue(data, "total_buy_quantity");
            standardData.open = getDoubleValue(data, "open");
            standardData.high = getDoubleValue(data, "high");
            standardData.low = getDoubleValue(data, "low");
            standardData.close = getDoubleValue(data, "close");
            standardData.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            standardData.source = "angelone";

            return standardData;
        } catch (Exception e) {
            throw new RuntimeException("Error transforming Angel One market data", e);
        }
    }

    /**
     * Transform Angel One holdings to standard format
     */
    @SuppressWarnings("unchecked")
    public List<StandardHolding> transformAngelOneHoldings(Object angelData) {
        try {
            List<StandardHolding> standardHoldings = new ArrayList<>();

            Map<String, Object> response = (Map<String, Object>) angelData;
            List<Map<String, Object>> holdings = (List<Map<String, Object>>) response.get("data");

            if (holdings != null) {
                for (Map<String, Object> holding : holdings) {
                    StandardHolding standardHolding = new StandardHolding();

                    standardHolding.symbol = getStringValue(holding, "symboltoken");
                    standardHolding.exchange = getStringValue(holding, "exchange");
                    standardHolding.tradingsymbol = getStringValue(holding, "tradingsymbol");
                    standardHolding.quantity = getLongValue(holding, "quantity");
                    standardHolding.averagePrice = getDoubleValue(holding, "averageprice");
                    standardHolding.currentPrice = getDoubleValue(holding, "ltp");
                    standardHolding.totalValue = getDoubleValue(holding, "totalbuyvalue");
                    standardHolding.pnl = getDoubleValue(holding, "pnl");
                    standardHolding.pnlPercentage = getDoubleValue(holding, "pnlpercentage");
                    standardHolding.source = "angelone";

                    standardHoldings.add(standardHolding);
                }
            }

            return standardHoldings;
        } catch (Exception e) {
            throw new RuntimeException("Error transforming Angel One holdings", e);
        }
    }

    // ============ UPSTOX DATA TRANSFORMATION ============

    /**
     * Transform Upstox market data to standard format
     */
    @SuppressWarnings("unchecked")
    public StandardMarketData transformUpstoxMarketData(Object upstoxData, String symbol) {
        try {
            Map<String, Object> response = (Map<String, Object>) upstoxData;
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            StandardMarketData standardData = new StandardMarketData();

            standardData.symbol = symbol;
            standardData.exchange = "NSE"; // Default, should be extracted from instrument key
            standardData.ltp = getDoubleValue(data, "last_price");
            standardData.bid = 0.0; // Upstox LTP API doesn't provide bid/ask
            standardData.ask = 0.0;
            standardData.volume = 0L; // Not available in LTP API
            standardData.open = 0.0; // Not available in LTP API
            standardData.high = 0.0; // Not available in LTP API
            standardData.low = 0.0; // Not available in LTP API
            standardData.close = 0.0; // Not available in LTP API
            standardData.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            standardData.source = "upstox";

            return standardData;
        } catch (Exception e) {
            throw new RuntimeException("Error transforming Upstox market data", e);
        }
    }

    /**
     * Transform Upstox holdings to standard format
     */
    @SuppressWarnings("unchecked")
    public List<StandardHolding> transformUpstoxHoldings(Object upstoxData) {
        try {
            List<StandardHolding> standardHoldings = new ArrayList<>();

            Map<String, Object> response = (Map<String, Object>) upstoxData;
            List<Map<String, Object>> holdings = (List<Map<String, Object>>) response.get("data");

            if (holdings != null) {
                for (Map<String, Object> holding : holdings) {
                    StandardHolding standardHolding = new StandardHolding();

                    standardHolding.symbol = getStringValue(holding, "instrument_token");
                    standardHolding.exchange = getStringValue(holding, "exchange");
                    standardHolding.tradingsymbol = getStringValue(holding, "trading_symbol");
                    standardHolding.quantity = getLongValue(holding, "quantity");
                    standardHolding.averagePrice = getDoubleValue(holding, "average_price");
                    standardHolding.currentPrice = getDoubleValue(holding, "last_price");
                    standardHolding.totalValue = standardHolding.quantity * standardHolding.currentPrice;
                    standardHolding.pnl = getDoubleValue(holding, "pnl");
                    standardHolding.pnlPercentage = calculatePnlPercentage(
                            standardHolding.averagePrice, standardHolding.currentPrice);
                    standardHolding.source = "upstox";

                    standardHoldings.add(standardHolding);
                }
            }

            return standardHoldings;
        } catch (Exception e) {
            throw new RuntimeException("Error transforming Upstox holdings", e);
        }
    }

    // ============ HISTORICAL DATA TRANSFORMATION ============

    /**
     * Transform historical data from any broker to standard format
     */
    @SuppressWarnings("unchecked")
    public StandardHistoricalData transformHistoricalData(Object brokerData, String symbol,
            String exchange, String interval, String source) {
        try {
            StandardHistoricalData standardData = new StandardHistoricalData();
            standardData.symbol = symbol;
            standardData.exchange = exchange;
            standardData.interval = interval;
            standardData.source = source;
            standardData.candles = new ArrayList<>();

            List<List<Object>> candleData = null;

            if ("zerodha".equals(source)) {
                Map<String, Object> response = (Map<String, Object>) brokerData;
                candleData = (List<List<Object>>) response.get("data");
            } else if ("angelone".equals(source)) {
                Map<String, Object> response = (Map<String, Object>) brokerData;
                candleData = (List<List<Object>>) response.get("data");
            } else if ("upstox".equals(source)) {
                Map<String, Object> response = (Map<String, Object>) brokerData;
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                candleData = (List<List<Object>>) data.get("candles");
            }

            if (candleData != null) {
                for (List<Object> candle : candleData) {
                    StandardCandle standardCandle = new StandardCandle();
                    standardCandle.time = candle.get(0).toString();
                    standardCandle.open = Double.valueOf(candle.get(1).toString());
                    standardCandle.high = Double.valueOf(candle.get(2).toString());
                    standardCandle.low = Double.valueOf(candle.get(3).toString());
                    standardCandle.close = Double.valueOf(candle.get(4).toString());
                    standardCandle.volume = Long.valueOf(candle.get(5).toString());

                    standardData.candles.add(standardCandle);
                }
            }

            return standardData;
        } catch (Exception e) {
            throw new RuntimeException("Error transforming historical data from " + source, e);
        }
    }

    // ============ UTILITY METHODS ============

    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : "";
    }

    private Double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null)
            return 0.0;
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null)
            return 0L;
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Double calculatePnlPercentage(Double averagePrice, Double currentPrice) {
        if (averagePrice == null || currentPrice == null || averagePrice == 0.0) {
            return 0.0;
        }
        return ((currentPrice - averagePrice) / averagePrice) * 100;
    }

    /**
     * Convert standardized data to JSON
     */
    public String toJson(Object standardData) {
        try {
            return objectMapper.writeValueAsString(standardData);
        } catch (Exception e) {
            throw new RuntimeException("Error converting to JSON", e);
        }
    }

    /**
     * Merge holdings from multiple brokers
     */
    public List<StandardHolding> mergeHoldings(List<StandardHolding> zerodhaHoldings,
            List<StandardHolding> angelOneHoldings,
            List<StandardHolding> upstoxHoldings) {
        List<StandardHolding> mergedHoldings = new ArrayList<>();

        if (zerodhaHoldings != null)
            mergedHoldings.addAll(zerodhaHoldings);
        if (angelOneHoldings != null)
            mergedHoldings.addAll(angelOneHoldings);
        if (upstoxHoldings != null)
            mergedHoldings.addAll(upstoxHoldings);

        return mergedHoldings;
    }

    /**
     * Calculate portfolio summary from standardized holdings
     */
    public Map<String, Object> calculatePortfolioSummary(List<StandardHolding> holdings) {
        Map<String, Object> summary = new HashMap<>();

        double totalValue = 0.0;
        double totalPnl = 0.0;
        double totalInvested = 0.0;

        for (StandardHolding holding : holdings) {
            totalValue += holding.totalValue != null ? holding.totalValue : 0.0;
            totalPnl += holding.pnl != null ? holding.pnl : 0.0;
            totalInvested += (holding.quantity != null && holding.averagePrice != null)
                    ? holding.quantity * holding.averagePrice
                    : 0.0;
        }

        double totalPnlPercentage = totalInvested > 0 ? (totalPnl / totalInvested) * 100 : 0.0;

        summary.put("totalValue", totalValue);
        summary.put("totalPnl", totalPnl);
        summary.put("totalPnlPercentage", totalPnlPercentage);
        summary.put("totalInvested", totalInvested);
        summary.put("holdingsCount", holdings.size());
        summary.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return summary;
    }

    /**
     * Transform Zerodha positions to standard format
     */
    public List<StandardPosition> transformZerodhaPositions(Object zerodhaData) {
        List<StandardPosition> standardPositions = new ArrayList<>();
        // Add transformation logic here for Zerodha positions
        // This would parse the zerodhaData object and convert to StandardPosition
        // objects
        return standardPositions;
    }

    /**
     * Transform Angel One positions to standard format
     */
    public List<StandardPosition> transformAngelOnePositions(Object angelData) {
        List<StandardPosition> standardPositions = new ArrayList<>();
        // Add transformation logic here for Angel One positions
        // This would parse the angelData object and convert to StandardPosition objects
        return standardPositions;
    }

    /**
     * Transform Upstox positions to standard format
     */
    public List<StandardPosition> transformUpstoxPositions(Object upstoxData) {
        List<StandardPosition> standardPositions = new ArrayList<>();
        // Add transformation logic here for Upstox positions
        // This would parse the upstoxData object and convert to StandardPosition
        // objects
        return standardPositions;
    }

    // Generic transform methods for unified API
    public List<StandardOrder> transformOrders(Object brokerData, String source) {
        switch (source.toLowerCase()) {
            case "zerodha":
                return transformZerodhaOrders(brokerData);
            case "angelone":
                return transformAngelOneOrders(brokerData);
            case "upstox":
                return transformUpstoxOrders(brokerData);
            default:
                return new ArrayList<>();
        }
    }

    public List<StandardHolding> transformHoldings(Object brokerData, String source) {
        switch (source.toLowerCase()) {
            case "zerodha":
                return transformZerodhaHoldings(brokerData);
            case "angelone":
                return transformAngelOneHoldings(brokerData);
            case "upstox":
                return transformUpstoxHoldings(brokerData);
            default:
                return new ArrayList<>();
        }
    }

    public List<StandardPosition> transformPositions(Object brokerData, String source) {
        switch (source.toLowerCase()) {
            case "zerodha":
                return transformZerodhaPositions(brokerData);
            case "angelone":
                return transformAngelOnePositions(brokerData);
            case "upstox":
                return transformUpstoxPositions(brokerData);
            default:
                return new ArrayList<>();
        }
    }

    // Individual broker transform methods for orders
    public List<StandardOrder> transformZerodhaOrders(Object zerodhaData) {
        List<StandardOrder> standardOrders = new ArrayList<>();
        // Add Zerodha order transformation logic here
        return standardOrders;
    }

    public List<StandardOrder> transformAngelOneOrders(Object angelData) {
        List<StandardOrder> standardOrders = new ArrayList<>();
        // Add Angel One order transformation logic here
        return standardOrders;
    }

    public List<StandardOrder> transformUpstoxOrders(Object upstoxData) {
        List<StandardOrder> standardOrders = new ArrayList<>();
        // Add Upstox order transformation logic here
        return standardOrders;
    }
}