package com.urva.myfinance.coinTrack.Controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.Model.Watchlist;
import com.urva.myfinance.coinTrack.Service.WatchlistService;
import com.urva.myfinance.coinTrack.ResourceNotFoundException;

@RestController
@RequestMapping("/api/watchlists")
@CrossOrigin(origins = "*")
public class WatchlistController {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistController.class);

    @Autowired
    private WatchlistService watchlistService;

    /**
     * Get all watchlists for a user
     * GET /api/watchlists?userId=12345
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserWatchlists(@RequestParam String userId) {
        List<Watchlist> watchlists = watchlistService.getUserWatchlists(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of(
                        "watchlists", watchlists,
                        "count", watchlists.size()),
                "message", "Watchlists retrieved successfully"));
    }

    /**
     * Get specific watchlist by ID
     * GET /api/watchlists/67890
     */
    @GetMapping("/{watchlistId}")
    public ResponseEntity<Map<String, Object>> getWatchlist(@PathVariable String watchlistId) {
        Optional<Watchlist> watchlistOpt = watchlistService.getWatchlistById(watchlistId);

        if (watchlistOpt.isEmpty()) {
            throw new ResourceNotFoundException("Watchlist", watchlistId);
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", watchlistOpt.get(),
                "message", "Watchlist retrieved successfully"));
    }

    /**
     * Get watchlist with live market data
     * GET /api/watchlists/67890/market-data
     */
    @GetMapping("/{watchlistId}/market-data")
    public ResponseEntity<Map<String, Object>> getWatchlistWithMarketData(@PathVariable String watchlistId) {
        Map<String, Object> watchlistData = watchlistService.getWatchlistWithMarketData(watchlistId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", watchlistData,
                "message", "Watchlist with market data retrieved successfully"));
    }

    /**
     * Create a new watchlist
     * POST /api/watchlists
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWatchlist(@RequestBody CreateWatchlistRequest request) {
        Watchlist watchlist = watchlistService.createWatchlist(
                request.getUserId(),
                request.getName(),
                request.getDescription(),
                request.getColor());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", watchlist,
                "message", "Watchlist created successfully"));
    }

    /**
     * Update watchlist details
     * PUT /api/watchlists/67890
     */
    @PutMapping("/{watchlistId}")
    public ResponseEntity<Map<String, Object>> updateWatchlist(
            @PathVariable String watchlistId,
            @RequestBody UpdateWatchlistRequest request) {
        Watchlist watchlist = watchlistService.updateWatchlist(
                watchlistId,
                request.getName(),
                request.getDescription(),
                request.getColor());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", watchlist,
                "message", "Watchlist updated successfully"));
    }

    /**
     * Delete watchlist
     * DELETE /api/watchlists/67890
     */
    @DeleteMapping("/{watchlistId}")
    public ResponseEntity<Map<String, Object>> deleteWatchlist(@PathVariable String watchlistId) {
        watchlistService.deleteWatchlist(watchlistId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Watchlist deleted successfully"));
    }

    /**
     * Add symbol to watchlist
     * POST /api/watchlists/67890/symbols
     */
    @PostMapping("/{watchlistId}/symbols")
    public ResponseEntity<Map<String, Object>> addSymbolToWatchlist(
            @PathVariable String watchlistId,
            @RequestBody AddSymbolRequest request) {
        Watchlist watchlist = watchlistService.addSymbolToWatchlist(
                watchlistId,
                request.getSymbol(),
                request.getExchange(),
                request.getTradingSymbol(),
                request.getAlertPrice(),
                request.getAlertType(),
                request.getNotes());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", watchlist,
                "message", "Symbol added to watchlist successfully"));
    }

    /**
     * Remove symbol from watchlist
     * DELETE /api/watchlists/67890/symbols/{symbol}/{exchange}
     */
    @DeleteMapping("/{watchlistId}/symbols/{symbol}/{exchange}")
    public ResponseEntity<Map<String, Object>> removeSymbolFromWatchlist(
            @PathVariable String watchlistId,
            @PathVariable String symbol,
            @PathVariable String exchange) {
        Watchlist watchlist = watchlistService.removeSymbolFromWatchlist(watchlistId, symbol, exchange);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", watchlist,
                "message", "Symbol removed from watchlist successfully"));
    }

    /**
     * Set default watchlist for user
     * PUT /api/watchlists/67890/default?userId=12345
     */
    @PutMapping("/{watchlistId}/default")
    public ResponseEntity<Map<String, Object>> setDefaultWatchlist(
            @PathVariable String watchlistId,
            @RequestParam String userId) {
        watchlistService.setDefaultWatchlist(userId, watchlistId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Default watchlist updated successfully"));
    }

    // DTO Classes
    public static class CreateWatchlistRequest {
        private String userId;
        private String name;
        private String description;
        private String color;

        // Getters and setters
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    public static class UpdateWatchlistRequest {
        private String name;
        private String description;
        private String color;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    public static class AddSymbolRequest {
        private String symbol;
        private String exchange;
        private String tradingSymbol;
        private Double alertPrice;
        private String alertType;
        private String notes;

        // Getters and setters
        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getTradingSymbol() {
            return tradingSymbol;
        }

        public void setTradingSymbol(String tradingSymbol) {
            this.tradingSymbol = tradingSymbol;
        }

        public Double getAlertPrice() {
            return alertPrice;
        }

        public void setAlertPrice(Double alertPrice) {
            this.alertPrice = alertPrice;
        }

        public String getAlertType() {
            return alertType;
        }

        public void setAlertType(String alertType) {
            this.alertType = alertType;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}