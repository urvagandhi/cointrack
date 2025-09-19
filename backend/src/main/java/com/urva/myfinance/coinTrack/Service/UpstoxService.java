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

import com.urva.myfinance.coinTrack.Model.UpstoxAccount;
import com.urva.myfinance.coinTrack.Repository.UpstoxAccountRepository;

@Service
public class UpstoxService {

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
                .orElseThrow(() -> new RuntimeException("No Upstox account for user: " + appUserId));
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Upstox API key or redirect URI not set for user.");
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Upstox API credentials not properly set for user.");
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
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Upstox token exchange failed: " + responseJson.optString("message", "Unknown error"));
                }
            } else {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Upstox token exchange failed");
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Upstox token expired. Please re-authenticate.");
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch profile from Upstox");
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch holdings from Upstox");
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch positions from Upstox");
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch orders from Upstox");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox orders response", e);
        }
    }

    /**
     * Get historical candle data from Upstox
     */
    public Object getHistoricalData(String appUserId, String instrument_key, String interval, String to_date,
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch historical data from Upstox");
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to fetch LTP data from Upstox");
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
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to logout from Upstox");
            }
        } catch (JSONException e) {
            throw new IOException("Error parsing Upstox logout response", e);
        }
    }
}