package com.urva.myfinance.coinTrack.Controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.urva.myfinance.coinTrack.Model.AngelOneAccount;
import com.urva.myfinance.coinTrack.Service.AngelOneService;

@RestController
@RequestMapping("/angelone")
public class AngelOneController {

    private final AngelOneService angelOneService;

    public AngelOneController(AngelOneService angelOneService) {
        this.angelOneService = angelOneService;
    }

    /**
     * DTO for Angel One credentials
     */
    public static class AngelOneCredentialsDTO {
        public String appUserId;
        public String angelApiKey;
        public String angelClientId;
        public String angelPin;
    }

    /**
     * DTO for Angel One login
     */
    public static class AngelOneLoginDTO {
        public String appUserId;
        public String totp; // Optional TOTP for 2FA
    }

    /**
     * Set Angel One API credentials for a user
     */
    @PostMapping("/set-credentials")
    public ResponseEntity<?> setAngelOneCredentials(@RequestBody AngelOneCredentialsDTO credentials) {
        try {
            angelOneService.setAngelOneCredentials(
                    credentials.appUserId,
                    credentials.angelApiKey,
                    credentials.angelClientId,
                    credentials.angelPin);
            return ResponseEntity.ok("Angel One API credentials updated for user " + credentials.appUserId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update Angel One credentials: " + e.getMessage());
        }
    }

    /**
     * Login to Angel One and get JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginToAngelOne(@RequestBody AngelOneLoginDTO loginData) {
        try {
            AngelOneAccount account = angelOneService.loginToAngelOne(
                    loginData.appUserId,
                    loginData.totp);
            return ResponseEntity.ok(account);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to login to Angel One: " + e.getMessage());
        }
    }

    /**
     * Refresh JWT token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestParam String appUserId) {
        try {
            AngelOneAccount account = angelOneService.refreshToken(appUserId);
            return ResponseEntity.ok(account);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to refresh token: " + e.getMessage());
        }
    }

    /**
     * Get Angel One account status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getAngelOneStatus(@RequestParam String appUserId) {
        try {
            AngelOneAccount account = angelOneService.getAccountByAppUserId(appUserId);

            if (account.getJwtToken() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Angel One not connected for user: " + appUserId);
            }

            if (angelOneService.isTokenExpired(account)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Angel One session expired for user: " + appUserId);
            }

            String statusMessage = "Angel One connected for User = " + appUserId +
                    " (Client ID = " + account.getAngelClientId() + ")";
            return ResponseEntity.ok(statusMessage);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting Angel One status: " + e.getMessage());
        }
    }

    /**
     * Get user profile from Angel One
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam String appUserId) {
        try {
            Object profile = angelOneService.getProfile(appUserId);
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
     * Get holdings from Angel One
     */
    @GetMapping("/holdings")
    public ResponseEntity<?> getHoldings(@RequestParam String appUserId) {
        try {
            Object holdings = angelOneService.getHoldings(appUserId);
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
     * Get positions from Angel One
     */
    @GetMapping("/positions")
    public ResponseEntity<?> getPositions(@RequestParam String appUserId) {
        try {
            Object positions = angelOneService.getPositions(appUserId);
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
     * Get orders from Angel One
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@RequestParam String appUserId) {
        try {
            Object orders = angelOneService.getOrders(appUserId);
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
     * Get historical candle data from Angel One
     */
    @GetMapping("/historical")
    public ResponseEntity<?> getHistoricalData(
            @RequestParam String appUserId,
            @RequestParam String exchange,
            @RequestParam String symboltoken,
            @RequestParam String interval,
            @RequestParam String fromdate,
            @RequestParam String todate) {
        try {
            Object historicalData = angelOneService.getHistoricalData(
                    appUserId, exchange, symboltoken, interval, fromdate, todate);
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
     * Get LTP (Last Traded Price) data from Angel One
     */
    @GetMapping("/ltp")
    public ResponseEntity<?> getLTPData(
            @RequestParam String appUserId,
            @RequestParam String exchange,
            @RequestParam String tradingsymbol,
            @RequestParam String symboltoken) {
        try {
            Object ltpData = angelOneService.getLTPData(
                    appUserId, exchange, tradingsymbol, symboltoken);
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
}