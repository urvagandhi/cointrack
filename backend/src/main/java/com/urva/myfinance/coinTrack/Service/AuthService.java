package com.urva.myfinance.coinTrack.Service;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.urva.myfinance.coinTrack.Model.AngelOneAccount;
import com.urva.myfinance.coinTrack.Model.UpstoxAccount;
import com.urva.myfinance.coinTrack.Model.ZerodhaAccount;
import com.urva.myfinance.coinTrack.Repository.AngelOneAccountRepository;
import com.urva.myfinance.coinTrack.Repository.UpstoxAccountRepository;
import com.urva.myfinance.coinTrack.Repository.ZerodhaAccountRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // JWT Service Fields
    private String secretKey = "";

    // Token Refresh Service Fields
    @Autowired
    private AngelOneAccountRepository angelOneAccountRepository;

    @Autowired
    private UpstoxAccountRepository upstoxAccountRepository;

    @Autowired
    private ZerodhaAccountRepository zerodhaAccountRepository;

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(3);
    private volatile boolean isRunning = false;

    public AuthService() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            SecretKey sk = keyGenerator.generateKey();
            this.secretKey = Base64.getEncoder().encodeToString(sk.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate secret key for JWT", e);
        }
    }

    // ===============================
    // JWT TOKEN METHODS
    // ===============================

    public String generateToken(Authentication authentication) {
        try {
            Map<String, Object> claims = new HashMap<>();
            return Jwts.builder()
                    .claims(claims)
                    .subject(authentication.getName())
                    .issuedAt(new Date(System.currentTimeMillis()))
                    .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30))
                    .signWith(getKey())
                    .compact();
        } catch (InvalidKeyException e) {   
            throw new RuntimeException("Failed to generate JWT token for user: " + authentication.getName(), e);
        }
    }

    private Key getKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (DecodingException | WeakKeyException e) {
            throw new RuntimeException("Failed to decode secret key for JWT", e);
        }
    }

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract username from JWT token", e);
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract claim from JWT token", e);
        }
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to extract claims from token", e);
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract expiration date from JWT token", e);
        }
    }

    // ===============================
    // TOKEN REFRESH METHODS
    // ===============================

    /**
     * Initialize the token refresh service
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing Auth Service with Token Refresh...");

        isRunning = true;

        // Schedule token refresh tasks
        scheduleAngelOneTokenRefresh();
        scheduleUpstoxTokenRefresh();
        scheduleZerodhaTokenRefresh();

        logger.info("Auth Service initialized successfully");
    }

    /**
     * Schedule Angel One token refresh (JWT expires in 8 hours)
     */
    private void scheduleAngelOneTokenRefresh() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshAngelOneTokens();
            } catch (Exception e) {
                logger.error("Error in Angel One token refresh task", e);
            }
        }, 5, 30, TimeUnit.MINUTES);
    }

    /**
     * Schedule Upstox token refresh (Access token expires in 24 hours)
     */
    private void scheduleUpstoxTokenRefresh() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshUpstoxTokens();
            } catch (Exception e) {
                logger.error("Error in Upstox token refresh task", e);
            }
        }, 10, 120, TimeUnit.MINUTES);
    }

    /**
     * Schedule Zerodha token refresh (Access token expires in 6 hours)
     */
    private void scheduleZerodhaTokenRefresh() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshZerodhaTokens();
            } catch (Exception e) {
                logger.error("Error in Zerodha token refresh task", e);
            }
        }, 15, 60, TimeUnit.MINUTES);
    }

    /**
     * Refresh Angel One tokens that are expiring soon
     */
    private void refreshAngelOneTokens() {
        try {
            List<AngelOneAccount> accounts = angelOneAccountRepository.findAll();

            int refreshedCount = 0;
            int errorCount = 0;

            for (AngelOneAccount account : accounts) {
                try {
                    if (shouldRefreshAngelOneToken(account)) {
                        logger.info("Refreshing Angel One token for user: {}", account.getAppUserId());
                        // TODO: Implement token refresh logic
                        refreshedCount++;
                    }
                } catch (Exception e) {
                    logger.error("Failed to refresh Angel One token for user: {}", account.getAppUserId(), e);
                    errorCount++;
                }
            }

            if (refreshedCount > 0 || errorCount > 0) {
                logger.info("Angel One token refresh completed. Refreshed: {}, Errors: {}", refreshedCount, errorCount);
            }

        } catch (Exception e) {
            logger.error("Error during Angel One token refresh batch", e);
        }
    }

    /**
     * Refresh Upstox tokens that are expiring soon
     */
    private void refreshUpstoxTokens() {
        try {
            List<UpstoxAccount> accounts = upstoxAccountRepository.findAll();

            int refreshedCount = 0;
            int errorCount = 0;

            for (UpstoxAccount account : accounts) {
                try {
                    if (shouldRefreshUpstoxToken(account)) {
                        logger.info("Refreshing Upstox token for user: {}", account.getAppUserId());
                        // TODO: Implement token refresh logic
                        refreshedCount++;
                    }
                } catch (Exception e) {
                    logger.error("Failed to refresh Upstox token for user: {}", account.getAppUserId(), e);
                    errorCount++;
                }
            }

            if (refreshedCount > 0 || errorCount > 0) {
                logger.info("Upstox token refresh completed. Refreshed: {}, Errors: {}", refreshedCount, errorCount);
            }

        } catch (Exception e) {
            logger.error("Error during Upstox token refresh batch", e);
        }
    }

    /**
     * Refresh Zerodha tokens that are expiring soon
     */
    private void refreshZerodhaTokens() {
        try {
            List<ZerodhaAccount> accounts = zerodhaAccountRepository.findAll();

            int refreshedCount = 0;
            int errorCount = 0;

            for (ZerodhaAccount account : accounts) {
                try {
                    if (shouldRefreshZerodhaToken(account)) {
                        logger.info("Refreshing Zerodha token for user: {}", account.getAppUserId());
                        // TODO: Implement token refresh logic
                        refreshedCount++;
                    }
                } catch (Exception e) {
                    logger.error("Failed to refresh Zerodha token for user: {}", account.getAppUserId(), e);
                    errorCount++;
                }
            }

            if (refreshedCount > 0 || errorCount > 0) {
                logger.info("Zerodha token refresh completed. Refreshed: {}, Errors: {}", refreshedCount, errorCount);
            }

        } catch (Exception e) {
            logger.error("Error during Zerodha token refresh batch", e);
        }
    }

    private boolean shouldRefreshAngelOneToken(AngelOneAccount account) {
        if (account.getJwtToken() == null || account.getTokenExpiresAt() == null) {
            return false;
        }
        return account.getTokenExpiresAt().isBefore(LocalDateTime.now().plusHours(1));
    }

    private boolean shouldRefreshUpstoxToken(UpstoxAccount account) {
        if (account.getAccessToken() == null || account.getTokenExpiresAt() == null) {
            return false;
        }
        return account.getTokenExpiresAt().isBefore(LocalDateTime.now().plusHours(4));
    }

    private boolean shouldRefreshZerodhaToken(ZerodhaAccount account) {
        if (account.getKiteAccessToken() == null || account.getKiteTokenCreatedAt() == null) {
            return false;
        }
        // Zerodha tokens typically expire after 6 hours, refresh if older than 4 hours
        return account.getKiteTokenCreatedAt().isBefore(LocalDateTime.now().minusHours(4));
    }

    /**
     * Get token refresh status
     */
    public Map<String, Object> getTokenRefreshStatus() {
        try {
            long angelOneAccounts = angelOneAccountRepository.count();
            long upstoxAccounts = upstoxAccountRepository.count();
            long zerodhaAccounts = zerodhaAccountRepository.count();

            return Map.of(
                    "isRunning", isRunning,
                    "lastCheck", LocalDateTime.now(),
                    "accounts", Map.of(
                            "angelone", angelOneAccounts,
                            "upstox", upstoxAccounts,
                            "zerodha", zerodhaAccounts),
                    "refreshSchedule", Map.of(
                            "angelone", "Every 30 minutes",
                            "upstox", "Every 2 hours",
                            "zerodha", "Every 1 hour"));

        } catch (Exception e) {
            logger.error("Error getting token refresh status", e);
            throw new RuntimeException("Failed to get token refresh status", e);
        }
    }

    /**
     * Force refresh all tokens
     */
    public Map<String, Object> forceRefreshAllTokens() {
        try {
            logger.info("Force refreshing all broker tokens...");

            refreshAngelOneTokens();
            refreshUpstoxTokens();
            refreshZerodhaTokens();

            return Map.of(
                    "status", "completed",
                    "message", "Force refresh completed for all brokers",
                    "timestamp", LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Error during force refresh", e);
            throw new RuntimeException("Failed to force refresh tokens", e);
        }
    }

    /**
     * Check if token refresh service is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Shutdown the auth service
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Auth Service...");

        isRunning = false;

        try {
            scheduler.shutdown();

            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }

            logger.info("Auth Service shutdown completed");

        } catch (Exception e) {
            logger.error("Error during Auth Service shutdown", e);
            scheduler.shutdownNow();
        }
    }
}