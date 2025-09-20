package com.urva.myfinance.coinTrack.DTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for standardized data responses from broker APIs
 * Provides a unified format for data across different brokers
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandardizedDataResponse<T> {
    private String status; // "success", "error", "partial"
    private String message;
    private T data; // The actual standardized data
    private String dataType; // "holdings", "positions", "orders", "market_data"
    private String brokerName; // Source broker
    private LocalDateTime timestamp;
    private DataMetadata metadata;
    private List<String> errors;
    private List<String> warnings;

    /**
     * Metadata about the standardized data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataMetadata {
        private String version; // Standardization version
        private Integer totalRecords;
        private Integer processedRecords;
        private Integer failedRecords;
        private Map<String, Object> conversionRules; // Rules used for standardization
        private Map<String, Object> originalFormat; // Original data format info
        private List<String> appliedTransformations; // Transformations applied
    }

    /**
     * Market data specific response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketData {
        private String symbol;
        private String exchange;
        private Double ltp; // Last traded price
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Long volume;
        private LocalDateTime timestamp;
        private String brokerSource;
    }

    /**
     * Historical data response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalData {
        private String symbol;
        private String exchange;
        private String interval; // "1minute", "5minute", "day"
        private List<Candle> candles;
        private String brokerSource;
        private LocalDateTime fromDate;
        private LocalDateTime toDate;
    }

    /**
     * Candle data for historical data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candle {
        private LocalDateTime timestamp;
        private Double open;
        private Double high;
        private Double low;
        private Double close;
        private Long volume;
    }

    /**
     * Instrument data response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Instrument {
        private String symbol;
        private String exchange;
        private String isin;
        private String name;
        private String sector;
        private String instrumentType; // "EQUITY", "ETF", "MUTUAL_FUND"
        private Double lotSize;
        private Double tickSize;
        private String brokerSource;
    }

    // Constructors for different scenarios
    public StandardizedDataResponse(String status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public StandardizedDataResponse(String status, T data, String dataType, String brokerName) {
        this.status = status;
        this.data = data;
        this.dataType = dataType;
        this.brokerName = brokerName;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Create success response
     */
    public static <T> StandardizedDataResponse<T> success(T data, String dataType, String brokerName) {
        return new StandardizedDataResponse<>("success", data, dataType, brokerName);
    }

    /**
     * Create error response
     */
    public static <T> StandardizedDataResponse<T> error(String message, String brokerName) {
        StandardizedDataResponse<T> response = new StandardizedDataResponse<>("error", message);
        response.setBrokerName(brokerName);
        return response;
    }

    /**
     * Create partial response (some data succeeded, some failed)
     */
    public static <T> StandardizedDataResponse<T> partial(T data, String message, String dataType, String brokerName) {
        StandardizedDataResponse<T> response = new StandardizedDataResponse<>("partial", data, dataType, brokerName);
        response.setMessage(message);
        return response;
    }

    /**
     * Add error to the response
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new java.util.ArrayList<>();
        }
        this.errors.add(error);
    }

    /**
     * Add warning to the response
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new java.util.ArrayList<>();
        }
        this.warnings.add(warning);
    }

    /**
     * Check if response is successful
     */
    public boolean isSuccess() {
        return "success".equals(status);
    }

    /**
     * Check if response has errors
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Check if response has warnings
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}
