package com.urva.myfinance.coinTrack.DTO;

import java.time.LocalDateTime;

import com.urva.myfinance.coinTrack.Model.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication response DTO for successful login
 * Renamed from LoginResponse for better naming consistency
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private User user;
    private LocalDateTime expiresAt; // When the token expires
    private String tokenType; // Type of token (e.g., "Bearer")
    private java.util.List<String> permissions; // User permissions/roles
    private java.util.Map<String, Object> metadata; // Additional metadata

    // Constructor for backward compatibility
    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = user;
        this.tokenType = "Bearer";
        // Remove password from user for security
        if (this.user != null) {
            this.user.setPassword(null);
        }
    }

    // Setter override to ensure password is always removed
    public void setUser(User user) {
        this.user = user;
        // Remove password from user for security
        if (this.user != null) {
            this.user.setPassword(null);
        }
    }

    /**
     * Add metadata to the response
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Add permission to the response
     */
    public void addPermission(String permission) {
        if (this.permissions == null) {
            this.permissions = new java.util.ArrayList<>();
        }
        this.permissions.add(permission);
    }
}
