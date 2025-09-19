package com.urva.myfinance.coinTrack.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "upstox_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpstoxAccount {
    @Id
    private String id;

    private String appUserId; // Reference to your User._id

    // Per-user Upstox API credentials
    private String upstoxApiKey;
    private String upstoxApiSecret;
    private String upstoxRedirectUri;

    private String accessToken; // Access token from Upstox OAuth2
    private String refreshToken; // Refresh token for access token renewal
    private LocalDateTime tokenCreatedAt; // When token was issued
    private LocalDateTime tokenExpiresAt; // When token expires

    // Additional Upstox specific fields
    private String userId; // Upstox user ID
    private String userName; // Upstox user name
    private String userType; // Individual or business account
    private String email; // User email
    private String exchangeInfo; // Enabled exchanges (NSE, BSE, MCX, etc.)
    private String products; // Enabled products (CNC, MIS, NRML, etc.)
    private Boolean isActive; // Whether the connection is active

    @CreatedDate
    private LocalDate createdAt;

    @LastModifiedDate
    private LocalDate updatedAt;
}