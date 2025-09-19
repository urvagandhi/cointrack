package com.urva.myfinance.coinTrack.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.urva.myfinance.coinTrack.Service.DataStandardizationService.StandardHolding;
import com.urva.myfinance.coinTrack.Service.DataStandardizationService.StandardMarketData;
import com.urva.myfinance.coinTrack.Service.DataStandardizationService.StandardPosition;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

@Service
public class UnifiedPortfolioService {

    private final ZerodhaService zerodhaService;
    private final AngelOneService angelOneService;
    private final UpstoxService upstoxService;
    private final DataStandardizationService dataStandardizationService;

    public UnifiedPortfolioService(ZerodhaService zerodhaService,
            AngelOneService angelOneService,
            UpstoxService upstoxService,
            DataStandardizationService dataStandardizationService) {
        this.zerodhaService = zerodhaService;
        this.angelOneService = angelOneService;
        this.upstoxService = upstoxService;
        this.dataStandardizationService = dataStandardizationService;
    }

    /**
     * Get unified portfolio data from all connected brokers
     */
    public Map<String, Object> getUnifiedPortfolio(String appUserId) {
        Map<String, Object> unifiedPortfolio = new HashMap<>();
        List<StandardHolding> allHoldings = new ArrayList<>();
        List<String> connectedBrokers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Fetch holdings from Zerodha
        try {
            Object zerodhaHoldings = zerodhaService.getHoldings(appUserId);
            List<StandardHolding> transformedZerodhaHoldings = dataStandardizationService
                    .transformZerodhaHoldings(zerodhaHoldings);
            allHoldings.addAll(transformedZerodhaHoldings);
            connectedBrokers.add("zerodha");
        } catch (KiteException | IOException e) {
            errors.add("Zerodha: " + e.getMessage());
        }

        // Fetch holdings from Angel One
        try {
            Object angelOneHoldings = angelOneService.getHoldings(appUserId);
            List<StandardHolding> transformedAngelHoldings = dataStandardizationService
                    .transformAngelOneHoldings(angelOneHoldings);
            allHoldings.addAll(transformedAngelHoldings);
            connectedBrokers.add("angelone");
        } catch (Exception e) {
            errors.add("Angel One: " + e.getMessage());
        }

        // Fetch holdings from Upstox
        try {
            Object upstoxHoldings = upstoxService.getHoldings(appUserId);
            List<StandardHolding> transformedUpstoxHoldings = dataStandardizationService
                    .transformUpstoxHoldings(upstoxHoldings);
            allHoldings.addAll(transformedUpstoxHoldings);
            connectedBrokers.add("upstox");
        } catch (Exception e) {
            errors.add("Upstox: " + e.getMessage());
        }

        // Calculate portfolio summary
        Map<String, Object> portfolioSummary = dataStandardizationService.calculatePortfolioSummary(allHoldings);

        // Build unified response
        unifiedPortfolio.put("summary", portfolioSummary);
        unifiedPortfolio.put("holdings", allHoldings);
        unifiedPortfolio.put("connectedBrokers", connectedBrokers);
        unifiedPortfolio.put("errors", errors);
        unifiedPortfolio.put("status", "success");

        return unifiedPortfolio;
    }

    /**
     * Get unified positions from all connected brokers
     */
    public Map<String, Object> getUnifiedPositions(String appUserId) {
        Map<String, Object> unifiedPositions = new HashMap<>();
        List<StandardPosition> allPositions = new ArrayList<>();
        List<String> connectedBrokers = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Fetch positions from each broker
        try {
            Object zerodhaPositions = zerodhaService.getPositions(appUserId);
            List<StandardPosition> transformedZerodhaPositions = dataStandardizationService
                    .transformZerodhaPositions(zerodhaPositions);
            allPositions.addAll(transformedZerodhaPositions);
            connectedBrokers.add("zerodha");
        } catch (KiteException | IOException e) {
            errors.add("Zerodha: " + e.getMessage());
        }

        try {
            Object angelOnePositions = angelOneService.getPositions(appUserId);
            List<StandardPosition> transformedAngelOnePositions = dataStandardizationService
                    .transformAngelOnePositions(angelOnePositions);
            allPositions.addAll(transformedAngelOnePositions);
            connectedBrokers.add("angelone");
        } catch (Exception e) {
            errors.add("Angel One: " + e.getMessage());
        }

        try {
            Object upstoxPositions = upstoxService.getPositions(appUserId);
            List<StandardPosition> transformedUpstoxPositions = dataStandardizationService
                    .transformUpstoxPositions(upstoxPositions);
            allPositions.addAll(transformedUpstoxPositions);
            connectedBrokers.add("upstox");
        } catch (Exception e) {
            errors.add("Upstox: " + e.getMessage());
        }

        unifiedPositions.put("positions", allPositions);
        unifiedPositions.put("connectedBrokers", connectedBrokers);
        unifiedPositions.put("errors", errors);
        unifiedPositions.put("status", "success");

        return unifiedPositions;
    }

    /**
     * Get market data for a symbol from all available brokers
     */
    public Map<String, StandardMarketData> getUnifiedMarketData(String appUserId, String symbol) {
        Map<String, StandardMarketData> marketData = new HashMap<>();

        // Try to get market data from each broker
        try {
            // For Zerodha, you would need to implement LTP fetching
            // Object zerodhaLtp = zerodhaService.getLTP(appUserId, symbol);
            // StandardMarketData zerodhaMarketData =
            // dataStandardizationService.transformZerodhaMarketData(zerodhaLtp, symbol);
            // marketData.put("zerodha", zerodhaMarketData);
        } catch (Exception e) {
            // Log error but continue with other brokers
        }

        try {
            Object angelOneLtp = angelOneService.getLTPData(appUserId, "NSE", symbol, ""); // symboltoken needed
            StandardMarketData angelMarketData = dataStandardizationService.transformAngelOneMarketData(angelOneLtp,
                    symbol);
            marketData.put("angelone", angelMarketData);
        } catch (Exception e) {
            // Log error but continue
        }

        try {
            String instrumentKey = "NSE_EQ|" + symbol; // Upstox instrument key format
            Object upstoxLtp = upstoxService.getLTPData(appUserId, instrumentKey);
            StandardMarketData upstoxMarketData = dataStandardizationService.transformUpstoxMarketData(upstoxLtp,
                    symbol);
            marketData.put("upstox", upstoxMarketData);
        } catch (Exception e) {
            // Log error but continue
        }

        return marketData;
    }

    /**
     * Get broker connection status for a user
     */
    public Map<String, Object> getBrokerConnectionStatus(String appUserId) {
        Map<String, Object> status = new HashMap<>();

        // Check Zerodha connection
        Map<String, Object> zerodhaStatus = new HashMap<>();
        try {
            var zerodhaAccount = zerodhaService.getAccountByAppUserId(appUserId);
            zerodhaStatus.put("connected", zerodhaAccount.getKiteAccessToken() != null);
            zerodhaStatus.put("expired", zerodhaService.isTokenExpired(zerodhaAccount));
            zerodhaStatus.put("userId", zerodhaAccount.getKiteUserId());
        } catch (Exception e) {
            zerodhaStatus.put("connected", false);
            zerodhaStatus.put("error", e.getMessage());
        }

        // Check Angel One connection
        Map<String, Object> angelOneStatus = new HashMap<>();
        try {
            var angelAccount = angelOneService.getAccountByAppUserId(appUserId);
            angelOneStatus.put("connected", angelAccount.getJwtToken() != null);
            angelOneStatus.put("expired", angelOneService.isTokenExpired(angelAccount));
            angelOneStatus.put("userId", angelAccount.getAngelClientId());
        } catch (Exception e) {
            angelOneStatus.put("connected", false);
            angelOneStatus.put("error", e.getMessage());
        }

        // Check Upstox connection
        Map<String, Object> upstoxStatus = new HashMap<>();
        try {
            var upstoxAccount = upstoxService.getAccountByAppUserId(appUserId);
            upstoxStatus.put("connected", upstoxAccount.getAccessToken() != null);
            upstoxStatus.put("expired", upstoxService.isTokenExpired(upstoxAccount));
            upstoxStatus.put("userId", upstoxAccount.getUserId());
        } catch (Exception e) {
            upstoxStatus.put("connected", false);
            upstoxStatus.put("error", e.getMessage());
        }

        status.put("zerodha", zerodhaStatus);
        status.put("angelone", angelOneStatus);
        status.put("upstox", upstoxStatus);

        return status;
    }

    /**
     * Get historical data from the best available broker
     */
    public Map<String, Object> getUnifiedHistoricalData(String appUserId, String symbol,
            String interval, String fromDate, String toDate) {
        Map<String, Object> result = new HashMap<>();

        // Try Angel One first (usually has good historical data)
        try {
            Object angelData = angelOneService.getHistoricalData(appUserId, "NSE", "", interval, fromDate, toDate);
            var standardData = dataStandardizationService.transformHistoricalData(angelData, symbol, "NSE", interval,
                    "angelone");
            result.put("data", standardData);
            result.put("source", "angelone");
            result.put("status", "success");
            return result;
        } catch (Exception e) {
            result.put("angelone_error", e.getMessage());
        }

        // Try Upstox as fallback
        try {
            String instrumentKey = "NSE_EQ|" + symbol;
            Object upstoxData = upstoxService.getHistoricalData(appUserId, instrumentKey, interval, toDate, fromDate);
            var standardData = dataStandardizationService.transformHistoricalData(upstoxData, symbol, "NSE", interval,
                    "upstox");
            result.put("data", standardData);
            result.put("source", "upstox");
            result.put("status", "success");
            return result;
        } catch (Exception e) {
            result.put("upstox_error", e.getMessage());
        }

        // If both fail, try Zerodha (would need implementation)
        result.put("status", "error");
        result.put("message", "No broker available for historical data");

        return result;
    }
}