package com.urva.myfinance.coinTrack.Controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.urva.myfinance.coinTrack.Model.User;
import com.urva.myfinance.coinTrack.Model.ZerodhaAccount;
import com.urva.myfinance.coinTrack.Service.UserService;
import com.urva.myfinance.coinTrack.Service.ZerodhaService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

@RestController
@RequestMapping("/zerodha")
public class ZerodhaController {
    /**
     * Endpoint to get Zerodha login URL for a user (uses API key from DB)
     */
    @GetMapping("/login-url")
    public ResponseEntity<?> getZerodhaLoginUrl(@RequestParam String appUserId) {
        try {
            ZerodhaAccount account = zerodhaService.getAccountByAppUserId(appUserId);
            String apiKey = account.getZerodhaApiKey();
            if (apiKey == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("API key not set for user");
            }

            // Option 1: Basic URL - requires Zerodha Developer Console redirect URL to be
            // set to http://localhost:8080/zerodha/callback
            String url = "https://kite.zerodha.com/connect/login?api_key=" + apiKey + "&v=3&redirect_params="
                    + java.net.URLEncoder.encode("appUserId=" + appUserId, "UTF-8");

            // Option 2: If you prefer to specify redirect in URL (uncomment below and
            // comment above)
            // String callbackUrl =
            // java.net.URLEncoder.encode("http://localhost:8080/zerodha/callback?appUserId="
            // + appUserId, "UTF-8");
            // String url = "https://kite.zerodha.com/connect/login?api_key=" + apiKey +
            // "&v=3&redirect_url=" + callbackUrl;

            return ResponseEntity.ok(url);
        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate login URL: " + e.getMessage());
        }
    }

    /**
     * Zerodha callback endpoint - automatically handles request_token and exchanges
     * for access_token
     * 
     * IMPORTANT: You must configure this URL in your Zerodha Developer Console:
     * 1. Go to https://developers.zerodha.com/apps
     * 2. Edit your app
     * 3. Set Redirect URL to: http://localhost:8080/zerodha/callback
     * 4. For production, use: https://yourdomain.com/zerodha/callback
     */
    @GetMapping("/callback")
    public ResponseEntity<?> zerodhaCallback(
            @RequestParam String request_token,
            @RequestParam String appUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status) {
        try {
            if (!"success".equals(status)) {
                // Redirect to frontend with error
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "http://localhost:3000/zerodha?error=authentication_failed")
                        .build();
            }

            // Automatically exchange request_token for access_token
            zerodhaService.connectZerodha(request_token, appUserId);

            // Redirect to frontend with success
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/zerodha?success=true&connected=true")
                    .build();

        } catch (ResponseStatusException e) {
            // Redirect to frontend with error details
            String errorMsg = java.net.URLEncoder.encode(e.getReason(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/zerodha?error=" + errorMsg)
                    .build();
        } catch (IOException e) {
            // Redirect to frontend with IO error
            String errorMsg = java.net.URLEncoder.encode("IO Error: " + e.getMessage(),
                    java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/zerodha?error=" + errorMsg)
                    .build();
        } catch (KiteException e) {
            // Redirect to frontend with Kite API error
            String errorMsg = java.net.URLEncoder.encode("Kite API Error: " + e.getMessage(),
                    java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/zerodha?error=" + errorMsg)
                    .build();
        } catch (Exception e) {
            // Redirect to frontend with generic error
            String errorMsg = java.net.URLEncoder.encode("Failed to connect Zerodha: " + e.getMessage(),
                    java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:3000/zerodha?error=" + errorMsg)
                    .build();
        }
    }

    /**
     * Endpoint to set/update Zerodha API key and secret for a user (accepts JSON
     * body)
     */
    public static class ZerodhaCredentialsDTO {
        public String appUserId;
        public String zerodhaApiKey;
        public String zerodhaApiSecret;
    }

    @PostMapping("/set-credentials")
    public ResponseEntity<?> setZerodhaCredentials(
            @org.springframework.web.bind.annotation.RequestBody ZerodhaCredentialsDTO credentials) {
        try {
            zerodhaService.setZerodhaCredentials(credentials.appUserId, credentials.zerodhaApiKey,
                    credentials.zerodhaApiSecret);
            return ResponseEntity.ok("Zerodha API credentials updated for user " + credentials.appUserId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update Zerodha credentials: " + e.getMessage());
        }
    }

    private final ZerodhaService zerodhaService;
    private final UserService userService;

    public ZerodhaController(ZerodhaService zerodhaService, UserService userService) {
        this.zerodhaService = zerodhaService;
        this.userService = userService;
    }

    /**
     * Step 1: First-time connect / relogin (Frontend will send requestToken after
     * user authorizes via Zerodha login)
     * 
     * @param requestToken
     * @throws KiteException
     */
    @PostMapping("/connect")
    public ResponseEntity<?> connectZerodhaPost(
            @RequestParam String requestToken,
            @RequestParam String appUserId) {
        return connectZerodha(requestToken, appUserId);
    }

    @GetMapping("/connect")
    public ResponseEntity<?> connectZerodhaGet(
            @RequestParam String requestToken,
            @RequestParam String appUserId) {
        return connectZerodha(requestToken, appUserId);
    }

    private ResponseEntity<?> connectZerodha(String requestToken, String appUserId) {
        try {
            ZerodhaAccount account = zerodhaService.connectZerodha(requestToken, appUserId);
            return ResponseEntity.ok(account);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IO Error: " + e.getMessage());
        } catch (KiteException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kite API Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Step 2: Get active Kite client (test only, not for frontend)
     * You usually won't expose KiteConnect object directly,
     * instead fetch holdings/orders and return JSON.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getZerodhaStatus(@RequestParam String appUserId) {
        try {
            KiteConnect kite = zerodhaService.clientFor(appUserId);

            // Get the user name from UserService
            User user = userService.getUserById(appUserId);
            String appUserName = (user != null && user.getName() != null) ? user.getName() : appUserId;

            String statusMessage = "Zerodha linked for User = " + appUserName +
                    " (Zerodha ID = " + kite.getUserId() + ")";
            return ResponseEntity.ok(statusMessage);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (NullPointerException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting Zerodha status: " + e.getMessage());
        }
    }

    /**
     * Step 3: Example - fetch profile from Zerodha API
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam String appUserId) {
        try {
            KiteConnect kite = zerodhaService.clientFor(appUserId);
            Object profile = kite.getProfile();
            return ResponseEntity.ok(profile);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IO Error: " + e.getMessage());
        } catch (KiteException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kite API Error: " + e.getMessage());
        } catch (JSONException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching profile: " + e.getMessage());
        }
    }

    /**
     * Step 4: Example - fetch holdings from Zerodha API
     */
    @GetMapping("/stocks/holdings")
    public ResponseEntity<?> getHoldings(@RequestParam String appUserId) {
        try {
            Object holdings = zerodhaService.getHoldings(appUserId);
            return ResponseEntity.ok(holdings);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IO Error: " + e.getMessage());
        } catch (KiteException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kite API Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching holdings: " + e.getMessage());
        }
    }

    /**
     * Step 5: Example - fetch positions from Zerodha API
     */
    @GetMapping("/stocks/positions")
    public ResponseEntity<?> getPositions(@RequestParam String appUserId) {
        try {
            Object positions = zerodhaService.getPositions(appUserId);
            return ResponseEntity.ok(positions);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IO Error: " + e.getMessage());
        } catch (KiteException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kite API Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching positions: " + e.getMessage());
        }
    }

    /**
     * Step 6: Example - fetch orders from Zerodha API
     */
    @GetMapping("/stocks/orders")
    public ResponseEntity<?> getOrders(@RequestParam String appUserId) {
        try {
            Object orders = zerodhaService.getOrders(appUserId);
            return ResponseEntity.ok(orders);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IO Error: " + e.getMessage());
        } catch (KiteException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kite API Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching orders: " + e.getMessage());
        }
    }

    /**
     * Step 7: Example - fetch mutual fund holdings from Zerodha API
     */
    @GetMapping("/mf/holdings")
    public ResponseEntity<?> getMFHoldings(@RequestParam String appUserId) {
        try {
            Object mfHoldings = zerodhaService.getMFHoldings(appUserId);
            return ResponseEntity.ok(mfHoldings);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IO Error: " + e.getMessage());
        } catch (KiteException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kite API Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching MF holdings: " + e.getMessage());
        }
    }

    /**
     * Step 8: Example - fetch mutual fund orders from Zerodha API
     */
    @GetMapping("/mf/sips")
    public ResponseEntity<?> getSIPs(@RequestParam String appUserId) {
        try {
            Object sips = zerodhaService.getSIPs(appUserId);
            return ResponseEntity.ok(sips);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch SIPs: " + e.getMessage());
        }
    }

}
