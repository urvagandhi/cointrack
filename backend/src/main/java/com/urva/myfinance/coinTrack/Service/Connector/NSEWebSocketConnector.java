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
public class NSEWebSocketConnector {

    private static final Logger logger = LoggerFactory.getLogger(NSEWebSocketConnector.class);
    private static final String NSE_WS_URL = "wss://ws.nseindia.com/ws/data";

    @Autowired
    private LiveMarketDataRepository liveMarketDataRepository;

    private WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> subscribedSymbols = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2);
    private boolean isConnected = false;
    private boolean isReconnecting = false;

    /**
     * Initialize NSE WebSocket connection
     */
    public void initialize() {
        try {
            connect();
            scheduleHeartbeat();
            scheduleReconnection();
        } catch (Exception e) {
            logger.error("Failed to initialize NSE WebSocket connector", e);
        }
    }

    /**
     * Connect to NSE WebSocket
     */
    private void connect() {
        try {
            URI serverUri = new URI(NSE_WS_URL);

            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    logger.info("NSE WebSocket connected");
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
                        logger.error("Error processing NSE market data", e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.warn("NSE WebSocket closed. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
                    isConnected = false;

                    if (!isReconnecting) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception e) {
                    logger.error("NSE WebSocket error", e);
                    isConnected = false;
                }
            };

            webSocketClient.connect();

        } catch (Exception e) {
            logger.error("Failed to connect to NSE WebSocket", e);
        }
    }

    /**
     * Process incoming market data
     */
    private void processMarketData(String message) {
        try {
            JsonNode json = objectMapper.readTree(message);

            // Parse NSE market data format
            if (json.has("tk") && json.has("lp")) {
                String token = json.get("tk").asText();
                String symbol = getSymbolFromToken(token);

                if (symbol != null) {
                    LiveMarketData marketData = new LiveMarketData();
                    marketData.setSymbol(symbol);
                    marketData.setExchange("NSE");
                    marketData.setLtp(json.get("lp").asDouble());
                    marketData.setBid(json.has("bp1") ? json.get("bp1").asDouble() : null);
                    marketData.setAsk(json.has("sp1") ? json.get("sp1").asDouble() : null);
                    marketData.setVolume(json.has("v") ? json.get("v").asLong() : null);
                    marketData.setOpen(json.has("o") ? json.get("o").asDouble() : null);
                    marketData.setHigh(json.has("h") ? json.get("h").asDouble() : null);
                    marketData.setLow(json.has("l") ? json.get("l").asDouble() : null);
                    marketData.setClose(json.has("c") ? json.get("c").asDouble() : null);
                    marketData.setTimestamp(LocalDateTime.now());
                    marketData.setSource("nse");
                    marketData.setIsMarketOpen(true);
                    marketData.setTradingStatus("OPEN");

                    // Calculate change and change percent
                    if (marketData.getClose() != null && marketData.getLtp() != null) {
                        double change = marketData.getLtp() - marketData.getClose();
                        double changePercent = (change / marketData.getClose()) * 100;
                        marketData.setChange(change);
                        marketData.setChangePercent(changePercent);
                    }

                    // Save to database
                    liveMarketDataRepository.save(marketData);

                    logger.debug("Saved NSE market data for symbol: {}, LTP: {}", symbol, marketData.getLtp());
                }
            }

        } catch (Exception e) {
            logger.error("Failed to process NSE market data: {}", message, e);
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
            logger.info("Subscribed to NSE symbol: {}", symbol);
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
            logger.info("Unsubscribed from NSE symbol: {}", symbol);
        }
    }

    /**
     * Send subscription message
     */
    private void sendSubscription(String symbol) {
        try {
            String token = getTokenForSymbol(symbol);
            if (token != null) {
                Map<String, Object> subscribeMsg = Map.of(
                        "a", "subscribe",
                        "v", new String[] { token },
                        "m", "marketdata");

                String message = objectMapper.writeValueAsString(subscribeMsg);
                webSocketClient.send(message);

                logger.debug("Sent NSE subscription for symbol: {} (token: {})", symbol, token);
            }
        } catch (Exception e) {
            logger.error("Failed to send NSE subscription for symbol: {}", symbol, e);
        }
    }

    /**
     * Send unsubscription message
     */
    private void sendUnsubscription(String symbol) {
        try {
            String token = getTokenForSymbol(symbol);
            if (token != null) {
                Map<String, Object> unsubscribeMsg = Map.of(
                        "a", "unsubscribe",
                        "v", new String[] { token },
                        "m", "marketdata");

                String message = objectMapper.writeValueAsString(unsubscribeMsg);
                webSocketClient.send(message);

                logger.debug("Sent NSE unsubscription for symbol: {} (token: {})", symbol, token);
            }
        } catch (Exception e) {
            logger.error("Failed to send NSE unsubscription for symbol: {}", symbol, e);
        }
    }

    /**
     * Resubscribe to all symbols after reconnection
     */
    private void resubscribeSymbols() {
        for (String symbol : subscribedSymbols) {
            sendSubscription(symbol);
        }
        logger.info("Resubscribed to {} NSE symbols", subscribedSymbols.size());
    }

    /**
     * Schedule reconnection attempts
     */
    private void scheduleReconnect() {
        if (!isReconnecting) {
            isReconnecting = true;
            scheduler.schedule(() -> {
                logger.info("Attempting to reconnect to NSE WebSocket...");
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
                logger.info("NSE WebSocket not connected, attempting reconnection...");
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
                    Map<String, Object> pingMsg = Map.of("a", "ping");
                    String message = objectMapper.writeValueAsString(pingMsg);
                    webSocketClient.send(message);
                } catch (Exception e) {
                    logger.error("Failed to send NSE heartbeat", e);
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Get NSE token for symbol (mock implementation)
     * In real implementation, this would map symbols to NSE instrument tokens
     */
    private String getTokenForSymbol(String symbol) {
        // Mock token mapping - in real implementation, you'd have a symbol-to-token
        // mapping
        Map<String, String> symbolTokenMap = Map.of(
                "RELIANCE", "738561",
                "TCS", "2953217",
                "INFY", "408065",
                "HDFCBANK", "341249",
                "ICICIBANK", "4963329");

        return symbolTokenMap.get(symbol.toUpperCase());
    }

    /**
     * Get symbol from NSE token (mock implementation)
     */
    private String getSymbolFromToken(String token) {
        // Mock reverse mapping - in real implementation, you'd have a token-to-symbol
        // mapping
        Map<String, String> tokenSymbolMap = Map.of(
                "738561", "RELIANCE",
                "2953217", "TCS",
                "408065", "INFY",
                "341249", "HDFCBANK",
                "4963329", "ICICIBANK");

        return tokenSymbolMap.get(token);
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
        logger.info("NSE WebSocket connector shutdown");
    }
}