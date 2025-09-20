package com.urva.myfinance.coinTrack.Service;

import java.util.List;
import java.util.Map;

/**
 * Common interface for all broker services
 * Provides standardized methods for broker operations
 */
public interface BrokerService {

    /**
     * Connect to the broker (authenticate/initialize)
     * @param userId User identifier
     * @return Connection status and details
     */
    Map<String, Object> connect(String userId);

    /**
     * Check if the broker service is connected and authenticated
     * @param userId User identifier
     * @return true if connected, false otherwise
     */
    boolean isConnected(String userId);

    /**
     * Disconnect from the broker
     * @param userId User identifier
     * @return Disconnection status
     */
    Map<String, Object> disconnect(String userId);

    /**
     * Fetch user's holdings from the broker
     * @param userId User identifier
     * @return List of holdings
     */
    List<Map<String, Object>> fetchHoldings(String userId);

    /**
     * Fetch user's orders from the broker
     * @param userId User identifier
     * @return List of orders
     */
    List<Map<String, Object>> fetchOrders(String userId);

    /**
     * Fetch user's positions from the broker
     * @param userId User identifier
     * @return List of positions
     */
    List<Map<String, Object>> fetchPositions(String userId);

    /**
     * Place a new order
     * @param userId User identifier
     * @param orderDetails Order parameters
     * @return Order placement result
     */
    Map<String, Object> placeOrder(String userId, Map<String, Object> orderDetails);

    /**
     * Modify an existing order
     * @param userId User identifier
     * @param orderId Order identifier
     * @param modificationDetails Order modification parameters
     * @return Order modification result
     */
    Map<String, Object> modifyOrder(String userId, String orderId, Map<String, Object> modificationDetails);

    /**
     * Cancel an existing order
     * @param userId User identifier
     * @param orderId Order identifier
     * @return Order cancellation result
     */
    Map<String, Object> cancelOrder(String userId, String orderId);

    /**
     * Get account balance and margins
     * @param userId User identifier
     * @return Account balance and margin details
     */
    Map<String, Object> getAccountBalance(String userId);

    /**
     * Get user profile information
     * @param userId User identifier
     * @return User profile details
     */
    Map<String, Object> getUserProfile(String userId);

    /**
     * Get available instruments/stocks
     * @param userId User identifier
     * @return List of available instruments
     */
    List<Map<String, Object>> getInstruments(String userId);

    /**
     * Get real-time market data for instruments
     * @param userId User identifier
     * @param instruments List of instrument identifiers
     * @return Real-time market data
     */
    Map<String, Object> getMarketData(String userId, List<String> instruments);

    /**
     * Get historical data for an instrument
     * @param userId User identifier
     * @param instrument Instrument identifier
     * @param fromDate Start date (YYYY-MM-DD format)
     * @param toDate End date (YYYY-MM-DD format)
     * @param interval Data interval (1minute, 5minute, day, etc.)
     * @return Historical data
     */
    Map<String, Object> getHistoricalData(String userId, String instrument, String fromDate, String toDate, String interval);

    /**
     * Refresh authentication token if needed
     * @param userId User identifier
     * @return Token refresh result
     */
    Map<String, Object> refreshToken(String userId);

    /**
     * Get broker-specific configuration or settings
     * @param userId User identifier
     * @return Broker configuration
     */
    Map<String, Object> getBrokerConfig(String userId);

    /**
     * Validate instrument symbol
     * @param userId User identifier
     * @param symbol Instrument symbol to validate
     * @return Validation result with instrument details
     */
    Map<String, Object> validateInstrument(String userId, String symbol);

    /**
     * Get order book (market depth) for an instrument
     * @param userId User identifier
     * @param instrument Instrument identifier
     * @return Order book data
     */
    Map<String, Object> getOrderBook(String userId, String instrument);

    /**
     * Get trade history
     * @param userId User identifier
     * @param fromDate Start date (optional)
     * @param toDate End date (optional)
     * @return Trade history
     */
    List<Map<String, Object>> getTradeHistory(String userId, String fromDate, String toDate);

    /**
     * Get the broker name/identifier
     * @return Broker name (e.g., "zerodha", "angelone", "upstox")
     */
    String getBrokerName();

    /**
     * Get broker service status and health
     * @return Service status information
     */
    Map<String, Object> getServiceStatus();
}
