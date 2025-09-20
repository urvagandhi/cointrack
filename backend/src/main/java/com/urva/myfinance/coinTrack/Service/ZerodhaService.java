package com.urva.myfinance.coinTrack.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.json.JSONException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.urva.myfinance.coinTrack.Model.ZerodhaAccount;
import com.urva.myfinance.coinTrack.Repository.ZerodhaAccountRepository;
import com.urva.myfinance.coinTrack.ResourceNotFoundException;
import com.urva.myfinance.coinTrack.UnauthorizedException;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

@Service
public class ZerodhaService implements BrokerService {
    /**
     * Get ZerodhaAccount by appUserId
     * Throws ResourceNotFoundException if account not found
     */
    public ZerodhaAccount getAccountByAppUserId(String appUserId) {
        return zerodhaRepo.findByAppUserId(appUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Zerodha account not found for user: " + appUserId));
    }
    /**
     * Set or update Zerodha API key/secret for a user
     */
    public ZerodhaAccount setZerodhaCredentials(String appUserId, String apiKey, String apiSecret) {
        ZerodhaAccount account = zerodhaRepo.findByAppUserId(appUserId)
                .orElse(new ZerodhaAccount());
        account.setAppUserId(appUserId);
        account.setZerodhaApiKey(apiKey);
        account.setZerodhaApiSecret(apiSecret);
        return zerodhaRepo.save(account);
    }

    private final ZerodhaAccountRepository zerodhaRepo;

    // Removed hardcoded API key/secret. Now fetched per-user from DB.

    public ZerodhaService(ZerodhaAccountRepository zerodhaRepo) {
        this.zerodhaRepo = zerodhaRepo;
    }

    /**
     * First-time connect or refresh with request token
     */
    public ZerodhaAccount connectZerodha(String requestToken, String appUserId)
            throws IOException, KiteException {

        try {
            ZerodhaAccount account = zerodhaRepo.findByAppUserId(appUserId)
                    .orElse(new ZerodhaAccount());

            // Fetch API key/secret from DB (must be set previously)
            String apiKey = account.getZerodhaApiKey();
            String apiSecret = account.getZerodhaApiSecret();
            if (apiKey == null || apiSecret == null) {
                throw new UnauthorizedException("Zerodha API key/secret not set for user: " + appUserId);
            }

            KiteConnect kite = new KiteConnect(apiKey);
            com.zerodhatech.models.User kiteUser = kite.generateSession(requestToken, apiSecret);

            account.setAppUserId(appUserId);
            account.setZerodhaApiKey(apiKey);
            account.setZerodhaApiSecret(apiSecret);
            account.setKiteUserId(kiteUser.userId);
            account.setKiteAccessToken(kiteUser.accessToken);
            account.setKitePublicToken(kiteUser.publicToken);
            account.setKiteTokenCreatedAt(LocalDateTime.now());

            return zerodhaRepo.save(account);
        } catch (KiteException e) {
            throw new UnauthorizedException("Invalid or expired requestToken. Please re-login.", e);
        }
    }

    /**
     * Get authenticated Kite client for user (reused session)
     */

    public boolean isTokenExpired(ZerodhaAccount account) {
        return account.getKiteTokenCreatedAt() == null ||
                account.getKiteTokenCreatedAt().toLocalDate().isBefore(LocalDate.now());
    }

    public KiteConnect clientFor(String appUserId) {
        ZerodhaAccount account = zerodhaRepo.findByAppUserId(appUserId)
                .orElseThrow(
                        () -> new UnauthorizedException("Zerodha not linked for this user"));

        if (account.getKiteAccessToken() == null) {
            throw new UnauthorizedException("No active token. Please login again.");
        }

        if (isTokenExpired(account)) {
            throw new UnauthorizedException("Zerodha session expired. Please relogin.");
        }

            String apiKey = account.getZerodhaApiKey();
            if (apiKey == null) {
                throw new IllegalArgumentException("Zerodha API key not set for this user.");
            }
            KiteConnect kite = new KiteConnect(apiKey);
        kite.setUserId(account.getKiteUserId());
        kite.setAccessToken(account.getKiteAccessToken());
        return kite;
    }

    /**
     * Step 4: Example - fetch holdings from Zerodha API
     */
    public Object getHoldings(String appUserId) throws IOException, KiteException {
        try {
            KiteConnect kite = clientFor(appUserId);
            return kite.getHoldings();
        } catch (IOException | KiteException e) {
            throw e; // Re-throw specific exceptions
        } catch (JSONException e) {
            throw new RuntimeException("Unexpected error fetching holdings for user: " + appUserId, e);
        }
    }

    /**
     * Step 5: Example - fetch positions from Zerodha API
     */
    public Object getPositions(String appUserId) throws IOException, KiteException {
        try {
            KiteConnect kite = clientFor(appUserId);
            return kite.getPositions();
        } catch (IOException | KiteException e) {
            throw e; // Re-throw specific exceptions
        } catch (JSONException e) {
            throw new RuntimeException("Unexpected error fetching positions for user: " + appUserId, e);
        }
    }

    /**
     * Step 6: Example - fetch orders from Zerodha API
     */
    public Object getOrders(String appUserId) throws IOException, KiteException {
        try {
            KiteConnect kite = clientFor(appUserId);
            return kite.getOrders();
        } catch (IOException | KiteException e) {
            throw e; // Re-throw specific exceptions
        } catch (JSONException e) {
            throw new RuntimeException("Unexpected error fetching orders for user: " + appUserId, e);
        }
    }

    /**
     * Step 7: Example - fetch mutual fund holdings from Zerodha API
     */
    public Object getMFHoldings(String appUserId) throws IOException, KiteException {
        try {
            KiteConnect kite = clientFor(appUserId);
            return kite.getMFHoldings();
        } catch (IOException | KiteException e) {
            throw e; // Re-throw specific exceptions
        } catch (JSONException e) {
            throw new RuntimeException("Unexpected error fetching MF holdings for user: " + appUserId, e);
        }
    }

    // /**
    // * Step 8: Example - fetch SIP orders from Zerodha API
    // */
    // public Object getSIPOrders(String appUserId) throws IOException,
    // KiteException {
    // KiteConnect kite = clientFor(appUserId);
    // return kite.getSIP();
    // }

    // Fetch SIPs for the user using Zerodha MF API - REST API
    @SuppressWarnings("UseSpecificCatch")
    public Object getSIPs(String appUserId) {
        try {
            ZerodhaAccount account = zerodhaRepo.findByAppUserId(appUserId)
                    .orElseThrow(() -> new RuntimeException("Zerodha not linked for this user"));
            if (account.getKiteAccessToken() == null) {
                throw new IllegalStateException("No active token. Please login again.");
            }
            if (isTokenExpired(account)) {
                throw new IllegalStateException("Zerodha session expired. Please relogin.");
            }
            String url = "https://api.kite.trade/mf/sips";
            HttpHeaders headers = new HttpHeaders();
            String apiKey = account.getZerodhaApiKey();
            if (apiKey == null) {
                throw new IllegalStateException("Zerodha API key not set for this user.");
            }
            headers.set("Authorization", "token " + apiKey + ":" + account.getKiteAccessToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (RuntimeException e) {
            throw e; // Re-throw expected exceptions
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error fetching SIPs for user: " + appUserId, e);
        }
    }

    // BrokerService interface implementations
    @Override
    public java.util.Map<String, Object> connect(String userId) {
        try {
            ZerodhaAccount account = getAccountByAppUserId(userId);
            if (account.getKiteAccessToken() != null && !isTokenExpired(account)) {
                return java.util.Map.of(
                        "status", "success",
                        "message", "Already connected",
                        "userId", account.getKiteUserId(),
                        "connected", true);
            }
            return java.util.Map.of(
                    "status", "error",
                    "message", "Please use request token to connect",
                    "connected", false);
        } catch (Exception e) {
            return java.util.Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "connected", false);
        }
    }

    @Override
    public boolean isConnected(String userId) {
        try {
            ZerodhaAccount account = getAccountByAppUserId(userId);
            return account.getKiteAccessToken() != null && !isTokenExpired(account);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public java.util.Map<String, Object> disconnect(String userId) {
        try {
            ZerodhaAccount account = getAccountByAppUserId(userId);
            account.setKiteAccessToken(null);
            account.setKitePublicToken(null);
            zerodhaRepo.save(account);
            return java.util.Map.of(
                    "status", "success",
                    "message", "Disconnected successfully");
        } catch (Exception e) {
            return java.util.Map.of(
                    "status", "error",
                    "message", e.getMessage());
        }
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> fetchHoldings(String userId) {
        try {
            Object holdings = getHoldings(userId);
            return java.util.List.of(java.util.Map.of("data", holdings, "source", "zerodha"));
        } catch (IOException | KiteException e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> fetchOrders(String userId) {
        try {
            Object orders = getOrders(userId);
            return java.util.List.of(java.util.Map.of("data", orders, "source", "zerodha"));
        } catch (IOException | KiteException e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> fetchPositions(String userId) {
        try {
            Object positions = getPositions(userId);
            return java.util.List.of(java.util.Map.of("data", positions, "source", "zerodha"));
        } catch (IOException | KiteException e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Override
    public java.util.Map<String, Object> placeOrder(String userId, java.util.Map<String, Object> orderDetails) {
        return java.util.Map.of(
                "status", "not_implemented",
                "message", "Order placement not implemented yet");
    }

    @Override
    public java.util.Map<String, Object> modifyOrder(String userId, String orderId, java.util.Map<String, Object> modificationDetails) {
        return java.util.Map.of(
                "status", "not_implemented",
                "message", "Order modification not implemented yet");
    }

    @Override
    public java.util.Map<String, Object> cancelOrder(String userId, String orderId) {
        return java.util.Map.of(
                "status", "not_implemented",
                "message", "Order cancellation not implemented yet");
    }

    @Override
    public java.util.Map<String, Object> getAccountBalance(String userId) {
        return java.util.Map.of(
                "status", "not_implemented",
                "message", "Account balance not implemented yet");
    }

    @Override
    public java.util.Map<String, Object> getUserProfile(String userId) {
        try {
            ZerodhaAccount account = getAccountByAppUserId(userId);
            return java.util.Map.of(
                    "userId", account.getKiteUserId(),
                    "broker", "zerodha",
                    "connected", isConnected(userId),
                    "apiKey", account.getZerodhaApiKey() != null ? "***" : null);
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getInstruments(String userId) {
        return java.util.List.of(java.util.Map.of(
                "status", "not_implemented",
                "message", "Instruments list not implemented yet"));
    }

    @Override
    public java.util.Map<String, Object> getMarketData(String userId, java.util.List<String> instruments) {
        return java.util.Map.of(
                "status", "not_implemented",
                "message", "Market data not implemented yet");
    }

    @Override
    public java.util.Map<String, Object> getHistoricalData(String userId, String instrument, String fromDate, String toDate, String interval) {
        return java.util.Map.of(
                "status", "not_implemented",
                "message", "Historical data not implemented yet");
    }

    @Override
    public java.util.Map<String, Object> refreshToken(String userId) {
        return java.util.Map.of(
                "status", "not_supported",
                "message", "Zerodha tokens need manual refresh with request token");
    }

    @Override
    public java.util.Map<String, Object> getBrokerConfig(String userId) {
        try {
            ZerodhaAccount account = getAccountByAppUserId(userId);
            return java.util.Map.of(
                    "broker", "zerodha",
                    "hasApiKey", account.getZerodhaApiKey() != null,
                    "hasApiSecret", account.getZerodhaApiSecret() != null,
                    "hasToken", account.getKiteAccessToken() != null,
                    "tokenExpired", isTokenExpired(account));
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @Override
    public java.util.Map<String, Object> validateInstrument(String userId, String symbol) {
        return java.util.Map.of(
                "status", "not_implemented",
                "message", "Instrument validation not implemented yet");
    }

    @Override
    public java.util.Map<String, Object> getOrderBook(String userId, String instrument) {
        return java.util.Map.of(
                "status", "not_implemented",
                "message", "Order book not implemented yet");
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getTradeHistory(String userId, String fromDate, String toDate) {
        return java.util.List.of(java.util.Map.of(
                "status", "not_implemented",
                "message", "Trade history not implemented yet"));
    }

    @Override
    public String getBrokerName() {
        return "zerodha";
    }

    @Override
    public java.util.Map<String, Object> getServiceStatus() {
        return java.util.Map.of(
                "broker", "zerodha",
                "status", "active",
                "version", "1.0",
                "features", java.util.List.of("holdings", "positions", "orders", "mf_holdings", "sips"));
    }
}
