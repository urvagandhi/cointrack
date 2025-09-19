package com.urva.myfinance.coinTrack.Model;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "watchlists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Watchlist {
    @Id
    private String id;

    private String userId; // Reference to User._id
    private String name; // Watchlist name (e.g., "My Stocks", "High Growth", etc.)
    private String description; // Optional description
    private List<WatchlistSymbol> symbols; // List of symbols in this watchlist
    private Boolean isDefault; // Whether this is the default watchlist
    private String color; // UI color code for the watchlist

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WatchlistSymbol {
        private String symbol; // Stock symbol (e.g., "RELIANCE")
        private String exchange; // Exchange (NSE, BSE)
        private String tradingSymbol; // Full trading symbol
        private LocalDateTime addedAt; // When symbol was added to watchlist
        private Double alertPrice; // Optional price alert
        private String alertType; // ABOVE, BELOW, PERCENTAGE_CHANGE
        private Boolean alertEnabled; // Whether alert is active
        private String notes; // User notes about this symbol
    }
}