package com.urva.myfinance.coinTrack.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "live_market_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveMarketData {
    @Id
    private String id;

    @Indexed
    private String symbol; // e.g., "RELIANCE"

    @Indexed
    private String exchange; // "NSE" or "BSE"

    private Double ltp; // Last Traded Price
    private Double bid; // Bid price
    private Double ask; // Ask price
    private Long volume; // Total volume traded

    // OHLC data
    private Double open;
    private Double high;
    private Double low;
    private Double close; // Previous day close

    @Indexed
    private LocalDateTime timestamp; // When this tick was received

    @Indexed
    private String source; // "nse", "bse", "angelone", "upstox", "zerodha"

    // Additional market data fields
    private Double change; // Change from previous close
    private Double changePercent; // Change percentage
    private Long totalBuyQuantity; // Total buy quantity
    private Long totalSellQuantity; // Total sell quantity
    private Double averagePrice; // Average traded price
    private Long totalTradedVolume; // Total traded volume
    private Double totalTradedValue; // Total traded value

    // Market depth (optional - can be expanded later)
    private String marketDepth; // JSON string for buy/sell depth

    // For intraday tracking
    private Boolean isMarketOpen; // Whether market is currently open
    private String tradingStatus; // "OPEN", "CLOSED", "PRE_OPEN", "POST_CLOSE"

    // Constructor for basic market data
    public LiveMarketData(String symbol, String exchange, Double ltp, Double bid, Double ask,
            Long volume, Double open, Double high, Double low, Double close,
            String source) {
        this.symbol = symbol;
        this.exchange = exchange;
        this.ltp = ltp;
        this.bid = bid;
        this.ask = ask;
        this.volume = volume;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.source = source;
        this.timestamp = LocalDateTime.now();
        this.isMarketOpen = true;
        this.tradingStatus = "OPEN";
    }
}