package com.urva.myfinance.coinTrack.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.urva.myfinance.coinTrack.Model.LiveMarketData;
import com.urva.myfinance.coinTrack.Model.Watchlist;
import com.urva.myfinance.coinTrack.Repository.LiveMarketDataRepository;
import com.urva.myfinance.coinTrack.Repository.WatchlistRepository;
import com.urva.myfinance.coinTrack.ResourceNotFoundException;

@Service
public class WatchlistService {

    private static final Logger logger = LoggerFactory.getLogger(WatchlistService.class);

    @Autowired
    private WatchlistRepository watchlistRepository;

    @Autowired
    private LiveMarketDataRepository liveMarketDataRepository;

    /**
     * Get all watchlists for a user
     */
    public List<Watchlist> getUserWatchlists(String userId) {
        return watchlistRepository.findByUserId(userId);
    }

    /**
     * Get watchlist by ID
     */
    public Optional<Watchlist> getWatchlistById(String watchlistId) {
        return watchlistRepository.findById(watchlistId);
    }

    /**
     * Create a new watchlist
     */
    public Watchlist createWatchlist(String userId, String name, String description, String color) {
        // Check if this is the first watchlist for the user (make it default)
        long userWatchlistCount = watchlistRepository.countByUserId(userId);
        boolean isDefault = userWatchlistCount == 0;

        Watchlist watchlist = new Watchlist();
        watchlist.setUserId(userId);
        watchlist.setName(name);
        watchlist.setDescription(description);
        watchlist.setColor(color);
        watchlist.setIsDefault(isDefault);
        watchlist.setSymbols(List.of());
        watchlist.setCreatedAt(LocalDateTime.now());
        watchlist.setUpdatedAt(LocalDateTime.now());

        return watchlistRepository.save(watchlist);
    }

    /**
     * Update watchlist details
     */
    public Watchlist updateWatchlist(String watchlistId, String name, String description, String color) {
        Optional<Watchlist> watchlistOpt = watchlistRepository.findById(watchlistId);
        if (watchlistOpt.isEmpty()) {
            throw new ResourceNotFoundException("Watchlist", watchlistId);
        }

        Watchlist watchlist = watchlistOpt.get();
        if (name != null)
            watchlist.setName(name);
        if (description != null)
            watchlist.setDescription(description);
        if (color != null)
            watchlist.setColor(color);
        watchlist.setUpdatedAt(LocalDateTime.now());

        return watchlistRepository.save(watchlist);
    }

    /**
     * Delete watchlist
     */
    public void deleteWatchlist(String watchlistId) {
        watchlistRepository.deleteById(watchlistId);
    }

    /**
     * Add symbol to watchlist
     */
    public Watchlist addSymbolToWatchlist(String watchlistId, String symbol, String exchange,
            String tradingSymbol, Double alertPrice, String alertType, String notes) {

        Optional<Watchlist> watchlistOpt = watchlistRepository.findById(watchlistId);
        if (watchlistOpt.isEmpty()) {
            throw new RuntimeException("Watchlist not found: " + watchlistId);
        }

        Watchlist watchlist = watchlistOpt.get();
        List<Watchlist.WatchlistSymbol> symbols = watchlist.getSymbols();

        // Check if symbol already exists
        boolean symbolExists = symbols.stream()
                .anyMatch(s -> s.getSymbol().equals(symbol) && s.getExchange().equals(exchange));

        if (symbolExists) {
            throw new RuntimeException("Symbol already exists in watchlist: " + symbol);
        }

        // Create new symbol entry
        Watchlist.WatchlistSymbol newSymbol = new Watchlist.WatchlistSymbol();
        newSymbol.setSymbol(symbol);
        newSymbol.setExchange(exchange);
        newSymbol.setTradingSymbol(tradingSymbol);
        newSymbol.setAddedAt(LocalDateTime.now());
        newSymbol.setAlertPrice(alertPrice);
        newSymbol.setAlertType(alertType);
        newSymbol.setAlertEnabled(alertPrice != null);
        newSymbol.setNotes(notes);

        symbols.add(newSymbol);
        watchlist.setSymbols(symbols);
        watchlist.setUpdatedAt(LocalDateTime.now());

        return watchlistRepository.save(watchlist);
    }

    /**
     * Remove symbol from watchlist
     */
    public Watchlist removeSymbolFromWatchlist(String watchlistId, String symbol, String exchange) {
        Optional<Watchlist> watchlistOpt = watchlistRepository.findById(watchlistId);
        if (watchlistOpt.isEmpty()) {
            throw new RuntimeException("Watchlist not found: " + watchlistId);
        }

        Watchlist watchlist = watchlistOpt.get();
        List<Watchlist.WatchlistSymbol> symbols = watchlist.getSymbols();

        // Remove the symbol
        symbols.removeIf(s -> s.getSymbol().equals(symbol) && s.getExchange().equals(exchange));

        watchlist.setSymbols(symbols);
        watchlist.setUpdatedAt(LocalDateTime.now());

        return watchlistRepository.save(watchlist);
    }

    /**
     * Get watchlist with live market data
     */
    public Map<String, Object> getWatchlistWithMarketData(String watchlistId) {
        Optional<Watchlist> watchlistOpt = watchlistRepository.findById(watchlistId);
        if (watchlistOpt.isEmpty()) {
            throw new RuntimeException("Watchlist not found: " + watchlistId);
        }

        Watchlist watchlist = watchlistOpt.get();
        List<Map<String, Object>> symbolsWithData = watchlist.getSymbols().stream()
                .map(symbol -> {
                    Map<String, Object> symbolData = new java.util.HashMap<>();
                    symbolData.put("symbol", symbol.getSymbol());
                    symbolData.put("exchange", symbol.getExchange());
                    symbolData.put("trading_symbol", symbol.getTradingSymbol());
                    symbolData.put("added_at", symbol.getAddedAt());
                    symbolData.put("alert_price", symbol.getAlertPrice());
                    symbolData.put("alert_type", symbol.getAlertType());
                    symbolData.put("alert_enabled", symbol.getAlertEnabled());
                    symbolData.put("notes", symbol.getNotes());

                    // Get live market data
                    try {
                        Optional<LiveMarketData> marketDataOpt = liveMarketDataRepository
                                .findTopBySymbolAndExchangeOrderByTimestampDesc(symbol.getSymbol(),
                                        symbol.getExchange());

                        if (marketDataOpt.isPresent()) {
                            LiveMarketData marketData = marketDataOpt.get();
                            symbolData.put("ltp", marketData.getLtp());
                            symbolData.put("change", marketData.getChange());
                            symbolData.put("change_percent", marketData.getChangePercent());
                            symbolData.put("volume", marketData.getVolume());
                            symbolData.put("last_update", marketData.getTimestamp());

                            // Check alert condition
                            if (symbol.getAlertEnabled() && symbol.getAlertPrice() != null) {
                                boolean alertTriggered = false;
                                if ("ABOVE".equals(symbol.getAlertType())
                                        && marketData.getLtp() > symbol.getAlertPrice()) {
                                    alertTriggered = true;
                                } else if ("BELOW".equals(symbol.getAlertType())
                                        && marketData.getLtp() < symbol.getAlertPrice()) {
                                    alertTriggered = true;
                                }
                                symbolData.put("alert_triggered", alertTriggered);
                            }
                        } else {
                            symbolData.put("ltp", null);
                            symbolData.put("change", null);
                            symbolData.put("change_percent", null);
                            symbolData.put("volume", null);
                            symbolData.put("last_update", null);
                            symbolData.put("alert_triggered", false);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to fetch market data for {}: {}", symbol.getSymbol(), e.getMessage());
                        symbolData.put("market_data_error", e.getMessage());
                    }

                    return symbolData;
                })
                .collect(Collectors.toList());

        return Map.of(
                "id", watchlist.getId(),
                "name", watchlist.getName(),
                "description", watchlist.getDescription(),
                "color", watchlist.getColor(),
                "is_default", watchlist.getIsDefault(),
                "symbols", symbolsWithData,
                "symbol_count", symbolsWithData.size(),
                "created_at", watchlist.getCreatedAt(),
                "updated_at", watchlist.getUpdatedAt());
    }

    /**
     * Set default watchlist for user
     */
    public void setDefaultWatchlist(String userId, String watchlistId) {
        // Remove default flag from all user's watchlists
        List<Watchlist> userWatchlists = watchlistRepository.findByUserId(userId);
        userWatchlists.forEach(w -> {
            w.setIsDefault(false);
            watchlistRepository.save(w);
        });

        // Set the specified watchlist as default
        Optional<Watchlist> watchlistOpt = watchlistRepository.findById(watchlistId);
        if (watchlistOpt.isPresent()) {
            Watchlist watchlist = watchlistOpt.get();
            if (!watchlist.getUserId().equals(userId)) {
                throw new RuntimeException("Watchlist does not belong to user");
            }
            watchlist.setIsDefault(true);
            watchlistRepository.save(watchlist);
        }
    }
}