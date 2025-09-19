package com.urva.myfinance.coinTrack.Controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.urva.myfinance.coinTrack.Model.UpstoxAccount;
import com.urva.myfinance.coinTrack.Service.UpstoxService;

@RestController
@RequestMapping("/upstox")
public class UpstoxController {

    private final UpstoxService upstoxService;

    public UpstoxController(UpstoxService upstoxService) {
        this.upstoxService = upstoxService;
    }

    /**
     * DTO for Upstox credentials
     */
    public static class UpstoxCredentialsDTO {
        public String appUserId;
        public String upstoxApiKey;
        public String upstoxApiSecret;
        public String upstoxRedirectUri;
    }

    /**
     * Set Upstox API credentials for a user
     */
    @PostMapping("/set-credentials")
    public ResponseEntity<?> setUpstoxCredentials(@RequestBody UpstoxCredentialsDTO credentials) {
        try {
            upstoxService.setUpstoxCredentials(
                    credentials.appUserId,
                    credentials.upstoxApiKey,
                    credentials.upstoxApiSecret,
                    credentials.upstoxRedirectUri);
            return ResponseEntity.ok("Upstox API credentials updated for user " + credentials.appUserId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update Upstox credentials: " + e.getMessage());
        }
    }

    /**
     * Get Upstox OAuth2 authorization URL
     */
    @GetMapping("/auth-url")
    public ResponseEntity<?> getAuthorizationUrl(@RequestParam String appUserId) {
        try {
            String authUrl = upstoxService.getAuthorizationUrl(appUserId);
            return ResponseEntity.ok(authUrl);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate authorization URL: " + e.getMessage());
        }
    }

    /**
     * Handle Upstox OAuth2 callback - exchange authorization code for access token
     */
    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error) {

        if (error != null) {
            // Redirect to frontend with error
            String errorMsg;
            try {
                errorMsg = java.net.URLEncoder.encode("Upstox authorization failed: " + error, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                errorMsg = "authorization_failed";
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/upstox?error=" + errorMsg)
                    .build();
        }

        try {
            // Extract appUserId from state parameter
            String appUserId = state.replace("user_", "");

            // Exchange code for token
            upstoxService.exchangeCodeForToken(appUserId, code);

            // Redirect to frontend with success
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/upstox?success=true&connected=true")
                    .build();
        } catch (ResponseStatusException e) {
            // Redirect to frontend with error details
            String errorMsg;
            try {
                errorMsg = java.net.URLEncoder.encode(e.getReason(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                errorMsg = "token_exchange_failed";
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/upstox?error=" + errorMsg)
                    .build();
        } catch (IOException e) {
            // Redirect to frontend with IO error
            String errorMsg;
            try {
                errorMsg = java.net.URLEncoder.encode("IO Error: " + e.getMessage(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                errorMsg = "io_error";
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/upstox?error=" + errorMsg)
                    .build();
        } catch (Exception e) {
            // Redirect to frontend with generic error
            String errorMsg;
            try {
                errorMsg = java.net.URLEncoder.encode("Failed to connect Upstox: " + e.getMessage(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                errorMsg = "connection_failed";
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/upstox?error=" + errorMsg)
                    .build();
        }
    }

    /**
     * Get Upstox account status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getUpstoxStatus(@RequestParam String appUserId) {
        try {
            UpstoxAccount account = upstoxService.getAccountByAppUserId(appUserId);

            if (account.getAccessToken() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Upstox not connected for user: " + appUserId);
            }

            if (upstoxService.isTokenExpired(account)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Upstox session expired for user: " + appUserId);
            }

            String statusMessage = "Upstox connected for User = " + appUserId +
                    " (User ID = " + account.getUserId() + ")";
            return ResponseEntity.ok(statusMessage);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting Upstox status: " + e.getMessage());
        }
    }

    /**
     * Get user profile from Upstox
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam String appUserId) {
        try {
            Object profile = upstoxService.getProfile(appUserId);
            return ResponseEntity.ok(profile);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching profile: " + e.getMessage());
        }
    }

    /**
     * Get holdings from Upstox
     */
    @GetMapping("/holdings")
    public ResponseEntity<?> getHoldings(@RequestParam String appUserId) {
        try {
            Object holdings = upstoxService.getHoldings(appUserId);
            return ResponseEntity.ok(holdings);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching holdings: " + e.getMessage());
        }
    }

    /**
     * Get positions from Upstox
     */
    @GetMapping("/positions")
    public ResponseEntity<?> getPositions(@RequestParam String appUserId) {
        try {
            Object positions = upstoxService.getPositions(appUserId);
            return ResponseEntity.ok(positions);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching positions: " + e.getMessage());
        }
    }

    /**
     * Get orders from Upstox
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@RequestParam String appUserId) {
        try {
            Object orders = upstoxService.getOrders(appUserId);
            return ResponseEntity.ok(orders);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching orders: " + e.getMessage());
        }
    }

    /**
     * Get historical candle data from Upstox
     */
    @GetMapping("/historical")
    public ResponseEntity<?> getHistoricalData(
            @RequestParam String appUserId,
            @RequestParam String instrument_key,
            @RequestParam String interval,
            @RequestParam String to_date,
            @RequestParam String from_date) {
        try {
            Object historicalData = upstoxService.getHistoricalData(
                    appUserId, instrument_key, interval, to_date, from_date);
            return ResponseEntity.ok(historicalData);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching historical data: " + e.getMessage());
        }
    }

    /**
     * Get LTP (Last Traded Price) data from Upstox
     */
    @GetMapping("/ltp")
    public ResponseEntity<?> getLTPData(
            @RequestParam String appUserId,
            @RequestParam String instrument_key) {
        try {
            Object ltpData = upstoxService.getLTPData(appUserId, instrument_key);
            return ResponseEntity.ok(ltpData);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching LTP data: " + e.getMessage());
        }
    }

    /**
     * Logout from Upstox (revoke token)
     */
    @DeleteMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String appUserId) {
        try {
            upstoxService.logout(appUserId);
            return ResponseEntity.ok("Successfully logged out from Upstox for user " + appUserId);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error logging out: " + e.getMessage());
        }
    }
}