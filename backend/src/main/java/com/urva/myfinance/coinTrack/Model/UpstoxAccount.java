package com.urva.myfinance.coinTrack.Model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Document(collection = "upstox_accounts")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class UpstoxAccount extends BrokerAccount {

    // Upstox-specific API credentials
    private String upstoxApiKey;
    private String upstoxApiSecret;
    private String upstoxRedirectUri;

    // Upstox-specific token fields
    private String accessToken; // Access token from Upstox OAuth2
    private String refreshToken; // Refresh token for access token renewal

    // Additional Upstox specific fields
    private String userName; // Upstox user name
    private String userType; // Individual or business account
    private String email; // User email
    private String exchangeInfo; // Enabled exchanges (NSE, BSE, MCX, etc.)
    private String products; // Enabled products (CNC, MIS, NRML, etc.)

    // Note: userId, tokenCreatedAt, tokenExpiresAt, isActive are inherited from
    // BrokerAccount

    @Override
    public String getBrokerName() {
        return "upstox";
    }

    @Override
    public boolean hasCredentials() {
        return upstoxApiKey != null && upstoxApiSecret != null;
    }

    @Override
    public boolean hasValidToken() {
        return accessToken != null && !isTokenExpired();
    }

    // Convenience getters/setters for inherited fields with Upstox-specific names
    public String getUpstoxUserId() {
        return getUserId();
    }

    public void setUpstoxUserId(String upstoxUserId) {
        setUserId(upstoxUserId);
    }
}