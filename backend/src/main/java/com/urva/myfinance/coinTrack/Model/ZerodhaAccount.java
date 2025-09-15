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

@Document(collection = "zerodha_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZerodhaAccount {
    @Id
    private String id;

    private String appUserId; // Reference to your User._id

    // Per-user Zerodha API credentials
    private String zerodhaApiKey;
    private String zerodhaApiSecret;

    private String kiteUserId; // Zerodha's userId (e.g. "AB1234")
    private String kiteAccessToken; // Access token from Kite
    private String kitePublicToken; // Optional: useful for some API calls
    private LocalDateTime kiteTokenCreatedAt; // When token was issued

    @CreatedDate
    private LocalDate createdAt;

    @LastModifiedDate
    private LocalDate updatedAt;
}
