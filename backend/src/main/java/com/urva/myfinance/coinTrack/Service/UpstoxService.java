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

import com.urva.myfinance.coinTrack.Model.UpstoxAccount;
import com.urva.myfinance.coinTrack.Repository.UpstoxAccountRepository;
import com.urva.myfinance.coinTrack.ResourceNotFoundException;
import com.urva.myfinance.coinTrack.UnauthorizedException;

@Service
public class UpstoxService implements BrokerService {

    private final UpstoxAccountRepository upstoxRepo;
    private final RestTemplate restTemplate;

    // Upstox API base URLs
    private static final String UPSTOX_BASE_URL = "https://api.upstox.com/v2";
    private static final String UPSTOX_AUTH_URL = "https://api.upstox.com/v2/login/authorization/token";
    private static final String UPSTOX_PROFILE_URL = UPSTOX_BASE_URL + "/user/profile";
    private static final String UPSTOX_HOLDINGS_URL = UPSTOX_BASE_URL + "/portfolio/long-term-holdings";
    private static final String UPSTOX_POSITIONS_URL = UPSTOX_BASE_URL + "/portfolio/short-term-positions";
    private static final String UPSTOX_ORDERS_URL = UPSTOX_BASE_URL + "/order/retrieve-all";
    private static final String UPSTOX_HISTORICAL_URL = UPSTOX_BASE_URL + "/historical-candle";
    private static final String UPSTOX_LTP_URL = UPSTOX_BASE_URL + "/market-quote/ltp";
    private static final String UPSTOX_LOGOUT_URL = UPSTOX_BASE_URL + "/logout";

    public UpstoxService(UpstoxAccountRepository upstoxRepo) {
        this.upstoxRepo = upstoxRepo;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get UpstoxAccount by appUserId
     */
    public UpstoxAccount getAccountByAppUserId(String appUserId) {
        return upstoxRepo.findByAppUserId(appUserId)
                .orElseThrow(() -> new ResourceNotFoundException("UpstoxAccount", appUserId));
    }

    /**
     * Set or update Upstox API credentials for a user
     */
    public UpstoxAccount setUpstoxCredentials(String appUserId, String apiKey, String apiSecret, String redirectUri) {
        UpstoxAccount account = upstoxRepo.findByAppUserId(appUserId)
                .orElse(new UpstoxAccount());
        account.setAppUserId(appUserId);
        account.setUpstoxApiKey(apiKey);
        account.setUpstoxApiSecret(apiSecret);
        account.setUpstoxRedirectUri(redirectUri);
        return upstoxRepo.save(account);
    }

    /**
     * Generate Upstox OAuth2 authorization URL
     */
    public String getAuthorizationUrl(String appUserId) {
        UpstoxAccount account = getAccountByAppUserId(appUserId);

        if (account.getUpstoxApiKey() == null || account.getUpstoxRedirectUri() == null) {
            throw new IllegalArgumentException("Upstox API key or redirect URI not set for user.");
        }

        String state = "user_" + appUserId; // State parameter for security
        String authUrl = "https://api.upstox.com/v2/login/authorization/dialog" +
                "?response_type=code" +
                "&client_id=" + account.getUpstoxApiKey() +
                "&redirect_uri=" + account.getUpstoxRedirectUri() +
                "&state=" + state;

        return authUrl;
    }

    /**
     * Exchange authorization code for access token
     */
    public UpstoxAccount exchangeCodeForToken(String appUserId, String authorizationCode) throws IOException {
        try {
            UpstoxAccount account = getAccountByAppUserId(appUserId);

            String apiKey = account.getUpstoxApiKey();
            String apiSecret = account.getUpstoxApiSecret();
            String redirectUri = account.getUpstoxRedirectUri();

            if (apiKey == null || apiSecret == null || redirectUri == null) {
                throw new IllegalArgumentException("Upstox API credentials not properly set for user.");
            }

            // Prepare token exchange request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            Map<String, String> tokenData = new HashMap<>();
            tokenData.put("code", authorizationCode);
            tokenData.put("client_id", apiKey);
            tokenData.put("client_secret", apiSecret);
            tokenData.put("redirect_uri", redirectUri);
            tokenData.put("grant_type", "authorization_code");

            HttpEntity<Map<String, String>> request = new HttpEntity<>(tokenData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    UPSTOX_AUTH_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());

                if (responseJson.getString("status").equals("success")) {
                    JSONObject data = responseJson.getJSONObject("data");
                    String accessToken = data.getString("access_token");
                    String refreshToken = data.optString("refresh_token", null);
                    int expiresIn = data.getInt("expires_in");

                    // Update account with tokens
                    account.setAccessToken(accessToken);
                    account.setRefreshToken(refreshToken);
                    account.setTokenCreatedAt(LocalDateTime.now());
                    account.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
                    account.setIsActive(true);

                    // Get user profile to populate additional fields
                    try {
                        JSONObject profileData = getUserProfileData(accessToken);
                        if (profileData != null) {
                            account.setUserId(profileData.optString("user_id"));
                            account.setUserName(profileData.optString("user_name"));
                            account.setUserType(profileData.optString("user_type"));
                            account.setEmail(profileData.optString("email"));
                            account.setExchangeInfo(profileData.optString("exchanges", ""));
                        }
                    } catch (Exception e) {
                        // Profile fetch failed, but token exchange succeeded
                        System.err.println("Warning: Could not fetch user profile: " + e.getMessage());
                    }

                    return upstoxRepo.save(account);
                } else {
                    throw new UnauthorizedException("Upstox token exchange failed: " + responseJson.optString("message", "Unknown error"));
                }
            } else {
                throw new UnauthorizedException("Upstox token exchange failed");
            }

        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox response", e);
        }
    }

    /**
     * Helper method to get user profile data
     */
    private JSONObject getUserProfileData(String accessToken) throws IOException {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    UPSTOX_PROFILE_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                if (responseJson.getString("status").equals("success")) {
                    return responseJson.getJSONObject("data");
                }
            }
            return null;
        } catch (Exception e) {
            throw new IOException("Error fetching user profile", e);
        }
    }

    /**
     * Check if access token is expired
     */
    public boolean isTokenExpired(UpstoxAccount account) {
        return account.getTokenExpiresAt() == null ||
                account.getTokenExpiresAt().isBefore(LocalDateTime.now());
    }

    /**
     * Get authenticated headers for API calls
     */
    private HttpHeaders getAuthenticatedHeaders(UpstoxAccount account) throws IOException {
        if (isTokenExpired(account)) {
            throw new UnauthorizedException("Upstox token expired. Please re-authenticate.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + account.getAccessToken());
        headers.set("Accept", "application/json");

        return headers;
    }

    /**
     * Get user profile from Upstox
     */
    public Object getProfile(String appUserId) throws IOException {
        try {
            UpstoxAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    UPSTOX_PROFILE_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new RuntimeException("Failed to fetch profile from Upstox");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox profile response", e);
        }
    }

    /**
     * Get holdings from Upstox
     */
    public Object getHoldings(String appUserId) throws IOException {
        try {
            UpstoxAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    UPSTOX_HOLDINGS_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new RuntimeException("Failed to fetch holdings from Upstox");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox holdings response", e);
        }
    }

    /**
     * Get positions from Upstox
     */
    public Object getPositions(String appUserId) throws IOException {
        try {
            UpstoxAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    UPSTOX_POSITIONS_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new RuntimeException("Failed to fetch positions from Upstox");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox positions response", e);
        }
    }

    /**
     * Get orders from Upstox
     */
    public Object getOrders(String appUserId) throws IOException {
        try {
            UpstoxAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    UPSTOX_ORDERS_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new RuntimeException("Failed to fetch orders from Upstox");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox orders response", e);
        }
    }

    /**
     * Get historical candle data from Upstox (internal method)
     */
    public Object getHistoricalDataInternal(String appUserId, String instrument_key, String interval, String to_date,
            String from_date) throws IOException {
        try {
            UpstoxAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            String url = UPSTOX_HISTORICAL_URL + "/" + instrument_key + "/" + interval +
                    "?to_date=" + to_date + "&from_date=" + from_date;

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new RuntimeException("Failed to fetch historical data from Upstox");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox historical data response", e);
        }
    }

    /**
     * Get LTP (Last Traded Price) data from Upstox
     */
    public Object getLTPData(String appUserId, String instrument_key) throws IOException {
        try {
            UpstoxAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            String url = UPSTOX_LTP_URL + "?instrument_key=" + instrument_key;

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject responseJson = new JSONObject(response.getBody());
                return responseJson.toMap();
            } else {
                throw new RuntimeException("Failed to fetch LTP data from Upstox");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox LTP data response", e);
        }
    }

    /**
     * Logout from Upstox (revoke token)
     */
    public void logout(String appUserId) throws IOException {
        try {
            UpstoxAccount account = getAccountByAppUserId(appUserId);
            HttpHeaders headers = getAuthenticatedHeaders(account);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    UPSTOX_LOGOUT_URL, HttpMethod.DELETE, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                // Clear tokens from database
                account.setAccessToken(null);
                account.setRefreshToken(null);
                account.setTokenCreatedAt(null);
                account.setTokenExpiresAt(null);
                account.setIsActive(false);
                upstoxRepo.save(account);
            } else {
                throw new RuntimeException("Failed to logout from Upstox");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox logout response", e);
        }
    }

    // BrokerService interface implementations
    @Override
    public java.util.Map<String, Object> connect(String userId) {
        try {
            UpstoxAccount account = getAccountByAppUserId(userId);
            if (account.getAccessToken() != null && !isTokenExpired(account)) {
                return java.util.Map.of(
                        "status", "success",
                        "message", "Already connected",
                        "userId", account.getUserId(),
                        "connected", true);
            }
            return java.util.Map.of(
                    "status", "error",
                    "message", "Please use authorization code to connect",
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
            UpstoxAccount account = getAccountByAppUserId(userId);
            return account.getAccessToken() != null && !isTokenExpired(account);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public java.util.Map<String, Object> disconnect(String userId) {
        try {
            logout(userId);
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
            return java.util.List.of(java.util.Map.of("data", holdings, "source", "upstox"));
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
            return java.util.List.of(java.util.Map.of("data", orders, "source", "upstox"));
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
            return java.util.List.of(java.util.Map.of("data", positions, "source", "upstox"));
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
            UpstoxAccount account = getAccountByAppUserId(userId);
            return java.util.Map.of(
                    "userId", account.getUserId(),
                    "broker", "upstox",
                    "connected", isConnected(userId),
                    "apiKey", account.getUpstoxApiKey() != null ? "***" : null);
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
                "message", "Upstox tokens need manual refresh with authorization code");
    }

    @Override
    public java.util.Map<String, Object> getBrokerConfig(String userId) {
        try {
            UpstoxAccount account = getAccountByAppUserId(userId);
            return java.util.Map.of(
                    "broker", "upstox",
                    "hasApiKey", account.getUpstoxApiKey() != null,
                    "hasApiSecret", account.getUpstoxApiSecret() != null,
                    "hasToken", account.getAccessToken() != null,
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
        return "upstox";
    }

    @Override
    public java.util.Map<String, Object> getServiceStatus() {
        return java.util.Map.of(
                "broker", "upstox",
                "status", "active",
                "version", "1.0",
                "features", java.util.List.of("holdings", "positions", "orders", "ltp_data", "logout"));
    }
}