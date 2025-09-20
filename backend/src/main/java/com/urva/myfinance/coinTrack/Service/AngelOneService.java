package com.urva.myfinance.coinTrack.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.urva.myfinance.coinTrack.Model.AngelOneAccount;
import com.urva.myfinance.coinTrack.Repository.AngelOneAccountRepository;

@Service
public class AngelOneService implements BrokerService {

    private final AngelOneAccountRepository angelOneRepo;
    private final RestTemplate restTemplate;

    // Angel One SmartAPI base URLs
    private static final String ANGEL_BASE_URL = "https://apiconnect.angelbroking.com";
    private static final String ANGEL_LOGIN_URL = ANGEL_BASE_URL + "/rest/auth/angelbroking/user/v1/loginByPassword";
    private static final String ANGEL_PROFILE_URL = ANGEL_BASE_URL + "/rest/secure/angelbroking/user/v1/getProfile";
    private static final String ANGEL_HOLDINGS_URL = ANGEL_BASE_URL
            + "/rest/secure/angelbroking/portfolio/v1/getHolding";
    private static final String ANGEL_POSITIONS_URL = ANGEL_BASE_URL + "/rest/secure/angelbroking/order/v1/getPosition";
    private static final String ANGEL_ORDERS_URL = ANGEL_BASE_URL + "/rest/secure/angelbroking/order/v1/getOrderBook";
    private static final String ANGEL_HISTORICAL_URL = ANGEL_BASE_URL
            + "/rest/secure/angelbroking/historical/v1/getCandleData";
    private static final String ANGEL_LTP_URL = ANGEL_BASE_URL + "/rest/secure/angelbroking/order/v1/getLtpData";

    public AngelOneService(AngelOneAccountRepository angelOneRepo) {
        this.angelOneRepo = angelOneRepo;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get AngelOneAccount by appUserId
     */
    public AngelOneAccount getAccountByAppUserId(String appUserId) {
        return angelOneRepo.findByAppUserId(appUserId)
                .orElseThrow(() -> new RuntimeException("No Angel One account for user: " + appUserId));
    }

    /**
     * Set or update Angel One API credentials for a user
     */
    public AngelOneAccount setAngelOneCredentials(String appUserId, String apiKey, String clientId, String pin) {
        AngelOneAccount account = angelOneRepo.findByAppUserId(appUserId)
                .orElse(new AngelOneAccount());
        account.setAppUserId(appUserId);
        account.setAngelApiKey(apiKey);
        account.setAngelClientId(clientId);
        account.setAngelPin(pin);
        return angelOneRepo.save(account);
    }

    /**
     * Login to Angel One and get JWT token
     */
    public AngelOneAccount loginToAngelOne(String appUserId, String totp) throws IOException {
        try {
            AngelOneAccount account = angelOneRepo.findByAppUserId(appUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Angel One credentials not set for user."));

            String apiKey = account.getAngelApiKey();
            String clientId = account.getAngelClientId();
            String pin = account.getAngelPin();

            if (apiKey == null || clientId == null || pin == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Angel One API key, client ID, or PIN not set for user.");
            }

            // Prepare login request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-ClientLocalIP", "192.168.1.1");
            headers.set("X-ClientPublicIP", "192.168.1.1");
            headers.set("X-MACAddress", "00:00:00:00:00:00");
            headers.set("Accept", "application/json");
            headers.set("X-PrivateKey", apiKey);

            Map<String, String> loginData = new HashMap<>();
            loginData.put("clientcode", clientId);
            loginData.put("password", pin);
            loginData.put("totp", totp != null ? totp : "");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(loginData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    ANGEL_LOGIN_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());

                if (responseJson.getBoolean("status")) {
                    JSONObject data = responseJson.getJSONObject("data");
                    String jwtToken = data.getString("jwtToken");
                    String refreshToken = data.getString("refreshToken");

                    // Update account with tokens
                    account.setJwtToken(jwtToken);
                    account.setRefreshToken(refreshToken);
                    account.setTokenCreatedAt(LocalDateTime.now());
                    account.setTokenExpiresAt(LocalDateTime.now().plusHours(8)); // Angel One tokens expire in 8 hours
                    account.setUserId(clientId);
                    account.setIsActive(true);

                    return angelOneRepo.save(account);
                } else {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Angel One login failed: " + responseJson.getString("message"));
                }
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Angel One login failed");
            }

        } catch (JSONException e) {
            throw new IOException("Error parsing Angel One response", e);
        }
    }

    /**
     * Check if JWT token is expired
     */
    public boolean isTokenExpired(AngelOneAccount account) {
        return account.getTokenExpiresAt() == null ||
                account.getTokenExpiresAt().isBefore(LocalDateTime.now());
    }

    /**
     * Refresh JWT token using refresh token (internal method)
     */
    public AngelOneAccount refreshTokenInternal(String appUserId) throws IOException {
        AngelOneAccount account = getAccountByAppUserId(appUserId);

        if (account.getRefreshToken() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "No refresh token available. Please login again.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + account.getJwtToken());
            headers.set("X-PrivateKey", account.getAngelApiKey());

            Map<String, String> refreshData = new HashMap<>();
            refreshData.put("refreshToken", account.getRefreshToken());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(refreshData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    ANGEL_BASE_URL + "/rest/auth/angelbroking/jwt/v1/generateTokens",
                    HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());

                if (responseJson.getBoolean("status")) {
                    JSONObject data = responseJson.getJSONObject("data");
                    String newJwtToken = data.getString("jwtToken");
                    String newRefreshToken = data.getString("refreshToken");

                    account.setJwtToken(newJwtToken);
                    account.setRefreshToken(newRefreshToken);
                    account.setTokenCreatedAt(LocalDateTime.now());
                    account.setTokenExpiresAt(LocalDateTime.now().plusHours(8));

                    return angelOneRepo.save(account);
                } else {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Token refresh failed: " + responseJson.getString("message"));
                }
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token refresh failed");
            }

        } catch (JSONException e) {
            throw new IOException("Error parsing token refresh response", e);
        }
    }

    /**
     * Get authenticated headers for API calls
     */
    private HttpHeaders getAuthenticatedHeaders(AngelOneAccount account) throws IOException {
        if (isTokenExpired(account)) {
            account = refreshTokenInternal(account.getAppUserId());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + account.getJwtToken());
        headers.set("X-PrivateKey", account.getAngelApiKey());
        headers.set("X-ClientLocalIP", "192.168.1.1");
        headers.set("X-ClientPublicIP", "192.168.1.1");
        headers.set("X-MACAddress", "00:00:00:00:00:00");
        headers.set("Accept", "application/json");

        return headers;
    }

    /**
     * Get user profile from Angel One
     */
    public Object getProfile(String appUserId) throws IOException {
        try {
            AngelOneAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    ANGEL_PROFILE_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch profile from Angel One");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Angel One profile response", e);
        }
    }

    /**
     * Get holdings from Angel One
     */
    public Object getHoldings(String appUserId) throws IOException {
        try {
            AngelOneAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    ANGEL_HOLDINGS_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch holdings from Angel One");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Angel One holdings response", e);
        }
    }

    /**
     * Get positions from Angel One
     */
    public Object getPositions(String appUserId) throws IOException {
        try {
            AngelOneAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    ANGEL_POSITIONS_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch positions from Angel One");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Angel One positions response", e);
        }
    }

    /**
     * Get orders from Angel One
     */
    public Object getOrders(String appUserId) throws IOException {
        try {
            AngelOneAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    ANGEL_ORDERS_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch orders from Angel One");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Angel One orders response", e);
        }
    }

    /**
     * Get historical candle data from Angel One
     */
    public Object getHistoricalData(String appUserId, String exchange, String symboltoken,
            String interval, String fromdate, String todate) throws IOException {
        try {
            AngelOneAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            Map<String, String> requestData = new HashMap<>();
            requestData.put("exchange", exchange);
            requestData.put("symboltoken", symboltoken);
            requestData.put("interval", interval);
            requestData.put("fromdate", fromdate);
            requestData.put("todate", todate);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestData, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    ANGEL_HISTORICAL_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch historical data from Angel One");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Angel One historical data response", e);
        }
    }

    /**
     * Get LTP (Last Traded Price) data from Angel One
     */
    public Object getLTPData(String appUserId, String exchange, String tradingsymbol, String symboltoken)
            throws IOException {
        try {
            AngelOneAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            Map<String, String> requestData = new HashMap<>();
            requestData.put("exchange", exchange);
            requestData.put("tradingsymbol", tradingsymbol);
            requestData.put("symboltoken", symboltoken);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestData, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    ANGEL_LTP_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch LTP data from Angel One");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Angel One LTP data response", e);
        }
    }

    // BrokerService interface implementations
    @Override
    public java.util.Map<String, Object> connect(String userId) {
        try {
            AngelOneAccount account = getAccountByAppUserId(userId);
            if (account.getJwtToken() != null && !isTokenExpired(account)) {
                return java.util.Map.of(
                        "status", "success",
                        "message", "Already connected",
                        "userId", account.getAngelClientId(),
                        "connected", true);
            }
            return java.util.Map.of(
                    "status", "error",
                    "message", "Please use TOTP to login",
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
            AngelOneAccount account = getAccountByAppUserId(userId);
            return account.getJwtToken() != null && !isTokenExpired(account);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public java.util.Map<String, Object> disconnect(String userId) {
        try {
            AngelOneAccount account = getAccountByAppUserId(userId);
            account.setJwtToken(null);
            account.setRefreshToken(null);
            angelOneRepo.save(account);
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
            return java.util.List.of(java.util.Map.of("data", holdings, "source", "angelone"));
        } catch (IOException e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> fetchOrders(String userId) {
        try {
            Object orders = getOrders(userId);
            return java.util.List.of(java.util.Map.of("data", orders, "source", "angelone"));
        } catch (IOException e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return java.util.List.of(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> fetchPositions(String userId) {
        try {
            Object positions = getPositions(userId);
            return java.util.List.of(java.util.Map.of("data", positions, "source", "angelone"));
        } catch (IOException e) {
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
            AngelOneAccount account = getAccountByAppUserId(userId);
            return java.util.Map.of(
                    "userId", account.getAngelClientId(),
                    "broker", "angelone",
                    "connected", isConnected(userId),
                    "apiKey", account.getAngelApiKey() != null ? "***" : null);
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
        try {
            refreshTokenInternal(userId);
            return java.util.Map.of(
                    "status", "success",
                    "message", "Token refreshed successfully");
        } catch (Exception e) {
            return java.util.Map.of(
                    "status", "error",
                    "message", e.getMessage());
        }
    }

    @Override
    public java.util.Map<String, Object> getBrokerConfig(String userId) {
        try {
            AngelOneAccount account = getAccountByAppUserId(userId);
            return java.util.Map.of(
                    "broker", "angelone",
                    "hasApiKey", account.getAngelApiKey() != null,
                    "hasClientId", account.getAngelClientId() != null,
                    "hasToken", account.getJwtToken() != null,
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
        return "angelone";
    }

    @Override
    public java.util.Map<String, Object> getServiceStatus() {
        return java.util.Map.of(
                "broker", "angelone",
                "status", "active",
                "version", "1.0",
                "features", java.util.List.of("holdings", "positions", "orders", "ltp_data"));
    }
}