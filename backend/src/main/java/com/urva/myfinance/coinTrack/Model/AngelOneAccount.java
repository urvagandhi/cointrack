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

@Document(collection = "angelone_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AngelOneAccount {
    @Id
    private String id;

    private String appUserId; // Reference to your User._id

    // Per-user Angel One API credentials
    private String angelApiKey;
    private String angelClientId;
    private String angelPin;
    private String angelTotp; // Optional: for two-factor authentication

    private String jwtToken; // JWT token from Angel One
    private String refreshToken; // Refresh token for JWT renewal
    private LocalDateTime tokenCreatedAt; // When token was issued
    private LocalDateTime tokenExpiresAt; // When token expires

    // Additional Angel One specific fields
    private String userId; // Angel One's client code
    private String sessionToken; // Session token for feed subscription
    private Boolean isActive; // Whether the connection is active

    @CreatedDate
    private LocalDate createdAt;

    @LastModifiedDate
    private LocalDate updatedAt;
}