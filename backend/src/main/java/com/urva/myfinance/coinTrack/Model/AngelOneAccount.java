package com.urva.myfinance.coinTrack.Model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Document(collection = "angelone_accounts")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AngelOneAccount extends BrokerAccount {

    // Angel One-specific API credentials
    private String angelApiKey;
    private String angelClientId;
    private String angelPin;
    private String angelTotp; // Optional: for two-factor authentication

    // Angel One-specific token fields
    private String jwtToken; // JWT token from Angel One
    private String refreshToken; // Refresh token for JWT renewal
    private String sessionToken; // Session token for feed subscription

    // Note: angelClientId can be mapped to userId from BrokerAccount
    // Note: tokenCreatedAt and tokenExpiresAt are inherited from BrokerAccount

    @Override
    public String getBrokerName() {
        return "angelone";
    }

    @Override
    public boolean hasCredentials() {
        return angelApiKey != null && angelClientId != null && angelPin != null;
    }

    @Override
    public boolean hasValidToken() {
        return jwtToken != null && !isTokenExpired();
    }

    // Convenience getters/setters for inherited fields with AngelOne-specific names
    public String getAngelUserId() {
        return getUserId();
    }

    public void setAngelUserId(String angelUserId) {
        setUserId(angelUserId);
    }

    // Map angelClientId to the base userId field as well for consistency
    public void setAngelClientId(String angelClientId) {
        this.angelClientId = angelClientId;
        setUserId(angelClientId); // Keep userId in sync
    }

    @Override
    public void setUserId(String userId) {
        super.setUserId(userId);
        if (this.angelClientId == null) {
            this.angelClientId = userId;
        }
    }
}