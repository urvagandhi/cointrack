package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.DTO.PortfolioResponse;
import com.urva.myfinance.coinTrack.Service.BrokerService;
import com.urva.myfinance.coinTrack.Service.DataStandardizationService;
import com.urva.myfinance.coinTrack.ResourceNotFoundException;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioController {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioController.class);

    @Autowired
    private Map<String, BrokerService> brokerServices; // Spring will inject all BrokerService implementations

    @Autowired
    private DataStandardizationService dataStandardizationService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    /**
     * Get aggregated portfolio value across all broker accounts
     * GET /api/portfolio/value
     */
    @GetMapping("/value")
    public ResponseEntity<PortfolioResponse> getPortfolioValue(@RequestParam String userId) {
        PortfolioResponse response = new PortfolioResponse();
        response.setUserId(userId);
        response.setGeneratedAt(LocalDateTime.now());

        PortfolioResponse.PortfolioSummary summary = new PortfolioResponse.PortfolioSummary();
        double totalValue = 0.0;
        double totalInvestment = 0.0;
        double totalPnl = 0.0;

        // Aggregate data from all broker services
        for (BrokerService brokerService : brokerServices.values()) {
            try {
                List<Map<String, Object>> holdings = brokerService.fetchHoldings(userId);
                List<DataStandardizationService.StandardHolding> standardHoldings = dataStandardizationService
                        .transformHoldings(holdings, brokerService.getBrokerName());

                for (DataStandardizationService.StandardHolding holding : standardHoldings) {
                    if (holding.totalValue != null) {
                        totalValue += holding.totalValue;
                    }
                    if (holding.pnl != null) {
                        totalPnl += holding.pnl;
                    }
                    // Calculate investment (this would need more complex logic in real
                    // implementation)
                    if (holding.averagePrice != null && holding.quantity != null) {
                        totalInvestment += (holding.averagePrice * holding.quantity);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch holdings from {}: {}", brokerService.getBrokerName(), e.getMessage());
            }
        }

        summary.setTotalValue(totalValue);
        summary.setTotalInvestment(totalInvestment);
        summary.setTotalPnl(totalPnl);
        summary.setTotalPnlPercentage(totalInvestment > 0 ? (totalPnl / totalInvestment) * 100 : 0.0);

        response.setSummary(summary);

        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed portfolio with holdings, positions, and orders
     * GET /api/portfolio/details
     */
    @GetMapping("/details")
    public ResponseEntity<PortfolioResponse> getPortfolioDetails(@RequestParam String userId) {
        PortfolioResponse response = new PortfolioResponse();
        response.setUserId(userId);
        response.setGeneratedAt(LocalDateTime.now());

        // Process each broker asynchronously
        List<CompletableFuture<PortfolioResponse.BrokerPortfolio>> futures = brokerServices.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> {
                    String brokerName = entry.getKey();
                    BrokerService brokerService = entry.getValue();

                    PortfolioResponse.BrokerPortfolio brokerPortfolio = new PortfolioResponse.BrokerPortfolio();
                    brokerPortfolio.setBrokerName(brokerName);
                    brokerPortfolio.setLastUpdated(LocalDateTime.now());

                    try {
                        brokerPortfolio.setStatus("connected");

                        // Fetch and convert holdings
                        List<Map<String, Object>> holdings = brokerService.fetchHoldings(userId);
                        List<DataStandardizationService.StandardHolding> standardHoldings = dataStandardizationService
                                .transformHoldings(holdings, brokerName);
                        brokerPortfolio.setHoldings(convertToPortfolioHoldings(standardHoldings, brokerName));

                        // Fetch and convert positions
                        List<Map<String, Object>> positions = brokerService.fetchPositions(userId);
                        List<DataStandardizationService.StandardPosition> standardPositions = dataStandardizationService
                                .transformPositions(positions, brokerName);
                        brokerPortfolio.setPositions(convertToPortfolioPositions(standardPositions, brokerName));

                        // Fetch and convert orders
                        List<Map<String, Object>> orders = brokerService.fetchOrders(userId);
                        List<DataStandardizationService.StandardOrder> standardOrders = dataStandardizationService
                                .transformOrders(orders, brokerName);
                        brokerPortfolio.setOrders(convertToPortfolioOrders(standardOrders, brokerName));

                        // Calculate totals
                        double totalValue = standardHoldings.stream()
                                .mapToDouble(h -> h.totalValue != null ? h.totalValue : 0.0)
                                .sum();
                        double totalPnl = standardHoldings.stream()
                                .mapToDouble(h -> h.pnl != null ? h.pnl : 0.0)
                                .sum();

                        brokerPortfolio.setTotalValue(totalValue);
                        brokerPortfolio.setTotalPnl(totalPnl);

                    } catch (Exception e) {
                        logger.warn("Failed to fetch portfolio from {}: {}", brokerName, e.getMessage());
                        brokerPortfolio.setStatus("error");
                        brokerPortfolio.setErrorMessage("Failed to fetch data: " + e.getMessage());
                    }

                    return brokerPortfolio;
                }, executorService))
                .toList();

        // Wait for all to complete
        List<PortfolioResponse.BrokerPortfolio> brokerPortfolios = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        response.setBrokerPortfolios(brokerPortfolios);

        return ResponseEntity.ok(response);
    }

    /**
     * Get portfolio summary for a specific broker
     * GET /api/portfolio/broker/{brokerName}
     */
    @GetMapping("/broker/{brokerName}")
    public ResponseEntity<PortfolioResponse> getBrokerPortfolio(
            @PathVariable String brokerName,
            @RequestParam String userId) {

        PortfolioResponse response = new PortfolioResponse();
        response.setUserId(userId);
        response.setGeneratedAt(LocalDateTime.now());

        BrokerService brokerService = brokerServices.get(brokerName.toLowerCase());
        if (brokerService == null) {
            throw new ResourceNotFoundException("Broker", brokerName);
        }

        PortfolioResponse.BrokerPortfolio brokerPortfolio = new PortfolioResponse.BrokerPortfolio();
        brokerPortfolio.setBrokerName(brokerName);
        brokerPortfolio.setStatus("connected");
        brokerPortfolio.setLastUpdated(LocalDateTime.now());

        // Fetch and convert data
        List<Map<String, Object>> holdings = brokerService.fetchHoldings(userId);
        List<DataStandardizationService.StandardHolding> standardHoldings = dataStandardizationService
                .transformHoldings(holdings, brokerName);
        brokerPortfolio.setHoldings(convertToPortfolioHoldings(standardHoldings, brokerName));

        List<Map<String, Object>> positions = brokerService.fetchPositions(userId);
        List<DataStandardizationService.StandardPosition> standardPositions = dataStandardizationService
                .transformPositions(positions, brokerName);
        brokerPortfolio.setPositions(convertToPortfolioPositions(standardPositions, brokerName));

        List<Map<String, Object>> orders = brokerService.fetchOrders(userId);
        List<DataStandardizationService.StandardOrder> standardOrders = dataStandardizationService
                .transformOrders(orders, brokerName);
        brokerPortfolio.setOrders(convertToPortfolioOrders(standardOrders, brokerName));

        // Calculate totals
        double totalValue = standardHoldings.stream()
                .mapToDouble(h -> h.totalValue != null ? h.totalValue : 0.0)
                .sum();
        double totalPnl = standardHoldings.stream()
                .mapToDouble(h -> h.pnl != null ? h.pnl : 0.0)
                .sum();

        brokerPortfolio.setTotalValue(totalValue);
        brokerPortfolio.setTotalPnl(totalPnl);

        response.setBrokerPortfolios(List.of(brokerPortfolio));

        return ResponseEntity.ok(response);
    }

    // Helper methods to convert between DTOs
    private List<PortfolioResponse.Holding> convertToPortfolioHoldings(
            List<DataStandardizationService.StandardHolding> standardHoldings, String brokerName) {
        return standardHoldings.stream()
                .map(sh -> {
                    PortfolioResponse.Holding holding = new PortfolioResponse.Holding();
                    holding.setSymbol(sh.symbol);
                    holding.setExchange(sh.exchange);
                    holding.setQuantity(sh.quantity != null ? sh.quantity.intValue() : 0);
                    holding.setAveragePrice(sh.averagePrice);
                    holding.setCurrentPrice(sh.currentPrice);
                    holding.setTotalValue(sh.totalValue);
                    holding.setPnl(sh.pnl);
                    holding.setPnlPercentage(sh.pnlPercentage);
                    holding.setBrokerName(brokerName);
                    return holding;
                })
                .toList();
    }

    private List<PortfolioResponse.Position> convertToPortfolioPositions(
            List<DataStandardizationService.StandardPosition> standardPositions, String brokerName) {
        return standardPositions.stream()
                .map(sp -> {
                    PortfolioResponse.Position position = new PortfolioResponse.Position();
                    position.setSymbol(sp.symbol);
                    position.setExchange(sp.exchange);
                    position.setProduct(sp.product);
                    position.setQuantity(sp.quantity != null ? sp.quantity.intValue() : 0);
                    position.setAveragePrice(sp.averagePrice);
                    position.setCurrentPrice(sp.ltp);
                    position.setPnl(sp.pnl);
                    position.setBrokerName(brokerName);
                    return position;
                })
                .toList();
    }

    private List<PortfolioResponse.Order> convertToPortfolioOrders(
            List<DataStandardizationService.StandardOrder> standardOrders, String brokerName) {
        return standardOrders.stream()
                .map(so -> {
                    PortfolioResponse.Order order = new PortfolioResponse.Order();
                    order.setOrderId(so.orderId);
                    order.setSymbol(so.symbol);
                    order.setExchange(so.exchange);
                    order.setOrderType(so.orderType);
                    order.setQuantity(so.quantity != null ? so.quantity.intValue() : 0);
                    order.setPrice(so.price);
                    order.setStatus(so.status);
                    order.setBrokerName(brokerName);
                    return order;
                })
                .toList();
    }
}