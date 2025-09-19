package com.urva.myfinance.coinTrack.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.urva.myfinance.coinTrack.Model.Watchlist;

@Repository
public interface WatchlistRepository extends MongoRepository<Watchlist, String> {

    /**
     * Find all watchlists for a specific user
     */
    List<Watchlist> findByUserId(String userId);

    /**
     * Find user's default watchlist
     */
    Optional<Watchlist> findByUserIdAndIsDefaultTrue(String userId);

    /**
     * Find watchlist by user and name
     */
    Optional<Watchlist> findByUserIdAndName(String userId, String name);

    /**
     * Find watchlists containing a specific symbol
     */
    @Query("{ 'userId': ?0, 'symbols.symbol': ?1 }")
    List<Watchlist> findByUserIdAndSymbol(String userId, String symbol);

    /**
     * Count total watchlists for a user
     */
    long countByUserId(String userId);
}