package com.urva.myfinance.coinTrack.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for broker connection requests
 * Used when establishing connections to broker APIs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrokerConnectionRequest {
    private String brokerName; // "zerodha", "angelone", "upstox"
    private String userId; // Application user ID

    // Common connection fields
    private String apiKey;
    private String apiSecret;

    // Zerodha-specific fields
    private String requestToken; // For Zerodha OAuth flow

    // AngelOne-specific fields
    private String clientId; // AngelOne client ID
    private String pin; // AngelOne PIN
    private String totp; // AngelOne TOTP (optional)

    // Upstox-specific fields
    private String authorizationCode; // For Upstox OAuth flow
    private String redirectUri; // Upstox redirect URI

    // Additional connection metadata
    private java.util.Map<String, String> additionalParams; // For any broker-specific parameters
    private String deviceInfo; // Device information for security
    private String ipAddress; // IP address for logging

    /**
     * Add additional parameter
     */
    public void addAdditionalParam(String key, String value) {
        if (this.additionalParams == null) {
            this.additionalParams = new java.util.HashMap<>();
        }
        this.additionalParams.put(key, value);
    }

    /**
     * Get additional parameter
     */
    public String getAdditionalParam(String key) {
        return this.additionalParams != null ? this.additionalParams.get(key) : null;
    }

    /**
     * Check if this is a Zerodha connection request
     */
    public boolean isZerodhaRequest() {
        return "zerodha".equalsIgnoreCase(brokerName);
    }

    /**
     * Check if this is an AngelOne connection request
     */
    public boolean isAngelOneRequest() {
        return "angelone".equalsIgnoreCase(brokerName);
    }

    /**
     * Check if this is an Upstox connection request
     */
    public boolean isUpstoxRequest() {
        return "upstox".equalsIgnoreCase(brokerName);
    }
}
