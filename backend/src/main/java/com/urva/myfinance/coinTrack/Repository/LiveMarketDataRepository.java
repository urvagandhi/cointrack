package com.urva.myfinance.coinTrack.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.urva.myfinance.coinTrack.Model.LiveMarketData;

@Repository
public interface LiveMarketDataRepository extends MongoRepository<LiveMarketData, String> {

    // Find latest data for a symbol and exchange
    Optional<LiveMarketData> findTopBySymbolAndExchangeOrderByTimestampDesc(String symbol, String exchange);

    // Find latest data for a symbol (any exchange)
    Optional<LiveMarketData> findTopBySymbolOrderByTimestampDesc(String symbol);

    // Find all data for a symbol within time range
    List<LiveMarketData> findBySymbolAndTimestampBetweenOrderByTimestampDesc(String symbol,
            LocalDateTime start,
            LocalDateTime end);

    // Find latest data for multiple symbols
    @Query("{'symbol': {$in: ?0}}")
    List<LiveMarketData> findLatestBySymbols(List<String> symbols);

    // Find data by source
    List<LiveMarketData> findBySourceOrderByTimestampDesc(String source);

    // Find data for a symbol and exchange within time range
    List<LiveMarketData> findBySymbolAndExchangeAndTimestampBetweenOrderByTimestampDesc(String symbol,
            String exchange,
            LocalDateTime start,
            LocalDateTime end);

    // Find latest data for each symbol (aggregated)
    @Query(value = "{'symbol': ?0, 'exchange': ?1}", sort = "{'timestamp': -1}")
    List<LiveMarketData> findLatestBySymbolAndExchange(String symbol, String exchange);

    // Delete old data (for cleanup)
    void deleteByTimestampBefore(LocalDateTime cutoff);

    // Count total records
    long countBySymbol(String symbol);

    // Find symbols with recent activity
    @Query(value = "{'timestamp': {$gte: ?0}}", fields = "{'symbol': 1, 'exchange': 1}")
    List<LiveMarketData> findActiveSymbols(LocalDateTime since);

    // Find latest market data for multiple symbols with latest timestamp
    @Query(value = "{}", sort = "{'timestamp': -1}")
    List<LiveMarketData> findAllOrderByTimestampDesc();
}