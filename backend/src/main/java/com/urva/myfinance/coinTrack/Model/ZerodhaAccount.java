package com.urva.myfinance.coinTrack.Model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Document(collection = "zerodha_accounts")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ZerodhaAccount extends BrokerAccount {

    // Zerodha-specific API credentials
    private String zerodhaApiKey;
    private String zerodhaApiSecret;

    // Zerodha-specific token fields
    private String kiteAccessToken; // Access token from Kite
    private String kitePublicToken; // Optional: useful for some API calls

    // Note: kiteUserId is inherited as userId from BrokerAccount
    // Note: kiteTokenCreatedAt is inherited as tokenCreatedAt from BrokerAccount

    @Override
    public String getBrokerName() {
        return "zerodha";
    }

    @Override
    public boolean hasCredentials() {
        return zerodhaApiKey != null && zerodhaApiSecret != null;
    }

    @Override
    public boolean hasValidToken() {
        return kiteAccessToken != null && !isTokenExpired();
    }

    // Convenience getters/setters for inherited fields with Zerodha-specific names
    public String getKiteUserId() {
        return getUserId();
    }

    public void setKiteUserId(String kiteUserId) {
        setUserId(kiteUserId);
    }

    public java.time.LocalDateTime getKiteTokenCreatedAt() {
        return getTokenCreatedAt();
    }

    public void setKiteTokenCreatedAt(java.time.LocalDateTime kiteTokenCreatedAt) {
        setTokenCreatedAt(kiteTokenCreatedAt);
    }
}
