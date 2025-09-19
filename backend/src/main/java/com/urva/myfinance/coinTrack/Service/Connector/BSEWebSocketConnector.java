package com.urva.myfinance.coinTrack.Service.Connector;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urva.myfinance.coinTrack.Model.LiveMarketData;
import com.urva.myfinance.coinTrack.Repository.LiveMarketDataRepository;

@Service
public class BSEWebSocketConnector {

    private static final Logger logger = LoggerFactory.getLogger(BSEWebSocketConnector.class);
    private static final String BSE_WS_URL = "wss://ws.bseindia.com/ws/data";

    @Autowired
    private LiveMarketDataRepository liveMarketDataRepository;

    private WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);
    private boolean isConnected = false;
    private boolean isReconnecting = false;

    /**
     * Initialize BSE WebSocket connection
     */
    public void initialize() {
        try {
            connect();
            scheduleHeartbeat();
            scheduleReconnection();
        } catch (Exception e) {
            logger.error("Failed to initialize BSE WebSocket connector", e);
        }
    }

    /**
     * Connect to BSE WebSocket
     */
    private void connect() {
        try {
            URI serverUri = new URI(BSE_WS_URL);

            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    logger.info("BSE WebSocket connected");
                    isConnected = true;
                    isReconnecting = false;

                    // Resubscribe to all symbols
                    resubscribeSymbols();
                }

                @Override
                public void onMessage(String message) {
                    try {
                        processMarketData(message);
                    } catch (Exception e) {
                        logger.error("Error processing BSE market data", e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.warn("BSE WebSocket closed. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
                    isConnected = false;

                    if (!isReconnecting) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception e) {
                    logger.error("BSE WebSocket error", e);
                    isConnected = false;
                }
            };

            webSocketClient.connect();

        } catch (Exception e) {
            logger.error("Failed to connect to BSE WebSocket", e);
        }
    }

    /**
     * Process incoming market data
     */
    private void processMarketData(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            // Parse BSE market data format (similar to NSE but with BSE-specific fields)
            if (json.has("securityId") && json.has("lastPrice")) {
                String securityId = json.get("securityId").asText();
                String symbol = getSymbolFromSecurityId(securityId);

                if (symbol != null) {
                    LiveMarketData marketData = new LiveMarketData();
                    marketData.setSymbol(symbol);
                    marketData.setExchange("BSE");
                    marketData.setLtp(json.get("lastPrice").asDouble());
                    marketData.setBid(json.has("bidPrice") ? json.get("bidPrice").asDouble() : null);
                    marketData.setAsk(json.has("askPrice") ? json.get("askPrice").asDouble() : null);
                    marketData.setVolume(
                            json.has("totalTradedQuantity") ? json.get("totalTradedQuantity").asLong() : null);
                    marketData.setOpen(json.has("openPrice") ? json.get("openPrice").asDouble() : null);
                    marketData.setHigh(json.has("highPrice") ? json.get("highPrice").asDouble() : null);
                    marketData.setLow(json.has("lowPrice") ? json.get("lowPrice").asDouble() : null);
                    marketData.setClose(json.has("previousClose") ? json.get("previousClose").asDouble() : null);
                    marketData.setTimestamp(LocalDateTime.now());
                    marketData.setSource("bse");
                    marketData.setIsMarketOpen(true);
                    marketData.setTradingStatus("OPEN");

                    // Calculate change and change percent
                    if (marketData.getClose() != null && marketData.getLtp() != null) {
                        double change = marketData.getLtp() - marketData.getClose();
                        double changePercent = (change / marketData.getClose()) * 100;
                        marketData.setChange(change);
                        marketData.setChangePercent(changePercent);
                    }

                    // Additional BSE-specific fields
                    if (json.has("totalTradedValue")) {
                        marketData.setTotalTradedValue(json.get("totalTradedValue").asDouble());
                    }

                    // Save to database
                    liveMarketDataRepository.save(marketData);

                    logger.debug("Saved BSE market data for symbol: {}, LTP: {}", symbol, marketData.getLtp());
                }
            }

        } catch (Exception e) {
            logger.error("Failed to process BSE market data: {}", message, e);
        }
    }

    /**
     * Subscribe to a symbol
     */
    public void subscribe(String symbol) {
        if (subscribedSymbols.add(symbol)) {
            if (isConnected) {
                sendSubscription(symbol);
            }
            logger.info("Subscribed to BSE symbol: {}", symbol);
        }
    }

    /**
     * Unsubscribe from a symbol
     */
    public void unsubscribe(String symbol) {
        if (subscribedSymbols.remove(symbol)) {
            if (isConnected) {
                sendUnsubscription(symbol);
            }
            logger.info("Unsubscribed from BSE symbol: {}", symbol);
        }
    }

    /**
     * Send subscription message
     */
    private void sendSubscription(String symbol) {
        try {
            String securityId = getSecurityIdForSymbol(symbol);
            if (securityId != null) {
                Map<String, Object> subscribeMsg = Map.of(
                        "action", "subscribe",
                        "securityIds", new String[] { securityId },
                        "mode", "full");

                String message = objectMapper.writeValueAsString(subscribeMsg);
                webSocketClient.send(message);

                logger.debug("Sent BSE subscription for symbol: {} (securityId: {})", symbol, securityId);
            }
        } catch (Exception e) {
            logger.error("Failed to send BSE subscription for symbol: {}", symbol, e);
        }
    }

    /**
     * Send unsubscription message
     */
    private void sendUnsubscription(String symbol) {
        try {
            String securityId = getSecurityIdForSymbol(symbol);
            if (securityId != null) {
                Map<String, Object> unsubscribeMsg = Map.of(
                        "action", "unsubscribe",
                        "securityIds", new String[] { securityId });

                String message = objectMapper.writeValueAsString(unsubscribeMsg);
                webSocketClient.send(message);

                logger.debug("Sent BSE unsubscription for symbol: {} (securityId: {})", symbol, securityId);
            }
        } catch (Exception e) {
            logger.error("Failed to send BSE unsubscription for symbol: {}", symbol, e);
        }
    }

    /**
     * Resubscribe to all symbols after reconnection
     */
    private void resubscribeSymbols() {
        for (String symbol : subscribedSymbols) {
            sendSubscription(symbol);
        }
        logger.info("Resubscribed to {} BSE symbols", subscribedSymbols.size());
    }

    /**
     * Schedule reconnection attempts
     */
    private void scheduleReconnect() {
        if (!isReconnecting) {
            isReconnecting = true;
            scheduler.schedule(() -> {
                logger.info("Attempting to reconnect to BSE WebSocket...");
                connect();
            }, 5, TimeUnit.SECONDS);
        }
    }

    /**
     * Schedule periodic reconnection checks
     */
    private void scheduleReconnection() {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isConnected && !isReconnecting) {
                logger.info("BSE WebSocket not connected, attempting reconnection...");
                scheduleReconnect();
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Schedule heartbeat to keep connection alive
     */
    private void scheduleHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            if (isConnected && webSocketClient != null) {
                try {
                    Map<String, Object> pingMsg = Map.of("action", "ping");
                    String message = objectMapper.writeValueAsString(pingMsg);
                    webSocketClient.send(message);
                } catch (Exception e) {
                    logger.error("Failed to send BSE heartbeat", e);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Get BSE security ID for symbol (mock implementation)
     * In real implementation, this would map symbols to BSE security IDs
     */
    private String getSecurityIdForSymbol(String symbol) {
        // Mock security ID mapping - in real implementation, you'd have a
        // symbol-to-securityId mapping
        Map<String, String> symbolSecurityIdMap = Map.of(
                "RELIANCE", "500325",
                "TCS", "532540",
                "INFY", "500209",
                "HDFCBANK", "500180",
                "ICICIBANK", "532174");

        return symbolSecurityIdMap.get(symbol.toUpperCase());
    }

    /**
     * Get symbol from BSE security ID (mock implementation)
     */
    private String getSymbolFromSecurityId(String securityId) {
        // Mock reverse mapping - in real implementation, you'd have a
        // securityId-to-symbol mapping
        Map<String, String> securityIdSymbolMap = Map.of(
                "500325", "RELIANCE",
                "532540", "TCS",
                "500209", "INFY",
                "500180", "HDFCBANK",
                "532174", "ICICIBANK");

        return securityIdSymbolMap.get(securityId);
    }

    /**
     * Check if connector is connected
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Get subscribed symbols count
     */
    public int getSubscribedSymbolsCount() {
        return subscribedSymbols.size();
    }

    /**
     * Shutdown the connector
     */
    public void shutdown() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        scheduler.shutdown();
        logger.info("BSE WebSocket connector shutdown");
    }
}