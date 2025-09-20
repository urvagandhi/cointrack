package com.urva.myfinance.coinTrack.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for portfolio responses
 * Contains aggregated portfolio data from multiple brokers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    private String userId;
    private LocalDateTime generatedAt;
    private PortfolioSummary summary;
    private List<BrokerPortfolio> brokerPortfolios;
    private List<String> connectedBrokers;
    private List<String> errors;
    private Map<String, Object> metadata;

    /**
     * Portfolio summary containing aggregated values
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioSummary {
        private Double totalValue;
        private Double totalInvestment;
        private Double totalPnl;
        private Double totalPnlPercentage;
        private Integer totalHoldings;
        private Integer totalPositions;
        private Integer totalOrders;
        private Map<String, Double> sectorAllocation;
        private Map<String, Double> brokerAllocation;
    }

    /**
     * Individual broker portfolio data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BrokerPortfolio {
        private String brokerName;
        private String status; // "connected", "disconnected", "error"
        private Double totalValue;
        private Double totalPnl;
        private List<Holding> holdings;
        private List<Position> positions;
        private List<Order> orders;
        private String errorMessage;
        private LocalDateTime lastUpdated;
    }

    /**
     * Standardized holding data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Holding {
        private String symbol;
        private String exchange;
        private String isin;
        private Integer quantity;
        private Double averagePrice;
        private Double currentPrice;
        private Double totalValue;
        private Double pnl;
        private Double pnlPercentage;
        private String sector;
        private String brokerName;
    }

    /**
     * Standardized position data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private String symbol;
        private String exchange;
        private String product; // "MIS", "CNC", "NRML"
        private Integer quantity;
        private Double averagePrice;
        private Double currentPrice;
        private Double pnl;
        private String positionType; // "long", "short"
        private String brokerName;
    }

    /**
     * Standardized order data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Order {
        private String orderId;
        private String symbol;
        private String exchange;
        private String orderType; // "BUY", "SELL"
        private String product; // "MIS", "CNC", "NRML"
        private Integer quantity;
        private Double price;
        private String status; // "PENDING", "COMPLETE", "CANCELLED"
        private LocalDateTime orderTime;
        private String brokerName;
    }

    /**
     * Add broker portfolio to the response
     */
    public void addBrokerPortfolio(BrokerPortfolio brokerPortfolio) {
        if (this.brokerPortfolios == null) {
            this.brokerPortfolios = new java.util.ArrayList<>();
        }
        this.brokerPortfolios.add(brokerPortfolio);
    }

    /**
     * Add connected broker to the list
     */
    public void addConnectedBroker(String brokerName) {
        if (this.connectedBrokers == null) {
            this.connectedBrokers = new java.util.ArrayList<>();
        }
        this.connectedBrokers.add(brokerName);
    }

    /**
     * Add error to the list
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new java.util.ArrayList<>();
        }
        this.errors.add(error);
    }

    /**
     * Add metadata
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }
}
