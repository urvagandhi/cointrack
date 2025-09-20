package com.urva.myfinance.coinTrack.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Data;

/**
 * Abstract base class for all broker account models
 * Contains shared fields and common functionality
 */
@Data
public abstract class BrokerAccount {

    @Id
    protected String id;

    // Common fields for all broker accounts
    protected String appUserId; // Reference to your User._id

    // Token management fields
    protected LocalDateTime tokenCreatedAt; // When token was issued
    protected LocalDateTime tokenExpiresAt; // When token expires (optional)

    // Common metadata
    protected Boolean isActive; // Whether the connection is active
    protected String userId; // Broker-specific user ID

    @CreatedDate
    protected LocalDate createdAt;

    @LastModifiedDate
    protected LocalDate updatedAt;

    /**
     * Abstract method to get the broker name
     * 
     * @return The name of the broker (e.g., "zerodha", "angelone", "upstox")
     */
    public abstract String getBrokerName();

    /**
     * Abstract method to check if the account has valid credentials
     * 
     * @return true if credentials are set, false otherwise
     */
    public abstract boolean hasCredentials();

    /**
     * Abstract method to check if the account has a valid token
     * 
     * @return true if token exists and is not expired, false otherwise
     */
    public abstract boolean hasValidToken();

    /**
     * Common method to check if token is expired
     * 
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired() {
        if (tokenExpiresAt == null) {
            // If no expiry time set, assume token doesn't expire or use creation date
            return tokenCreatedAt == null || tokenCreatedAt.toLocalDate().isBefore(LocalDate.now());
        }
        return tokenExpiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Common method to get account status
     * 
     * @return Map containing account status information
     */
    public java.util.Map<String, Object> getAccountStatus() {
        return java.util.Map.of(
                "broker", getBrokerName(),
                "connected", hasValidToken(),
                "hasCredentials", hasCredentials(),
                "tokenExpired", isTokenExpired(),
                "isActive", isActive != null ? isActive : false,
                "userId", userId != null ? userId : "N/A");
    }
}
