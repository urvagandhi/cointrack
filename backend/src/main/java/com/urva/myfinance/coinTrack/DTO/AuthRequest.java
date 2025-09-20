package com.urva.myfinance.coinTrack.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication request DTO for user login
 * Renamed from LoginRequest for better naming consistency
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    private String usernameOrEmail;
    private String password;

    // Additional fields for enhanced authentication
    private String rememberMe; // Optional: for "remember me" functionality
    private String deviceInfo; // Optional: device information for security
    private String ipAddress; // Optional: IP address for logging

    // Constructor for backward compatibility
    public AuthRequest(String usernameOrEmail, String password) {
        this.usernameOrEmail = usernameOrEmail;
        this.password = password;
    }
}
