package com.urva.myfinance.coinTrack.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urva.myfinance.coinTrack.DTO.BrokerConnectionRequest;
import com.urva.myfinance.coinTrack.ResourceNotFoundException;
import com.urva.myfinance.coinTrack.Service.BrokerService;

@RestController
@RequestMapping("/api/brokers")
@CrossOrigin(origins = "*")
public class BrokerController {

    @Autowired
    private Map<String, BrokerService> brokerServices; // Dynamic dispatch map

    /**
     * Connect to a broker using BrokerConnectionRequest
     * POST /api/brokers/connect
     */
    @PostMapping("/connect")
    public ResponseEntity<?> connectBroker(@RequestBody BrokerConnectionRequest request) {
        BrokerService brokerService = brokerServices.get(request.getBrokerName().toLowerCase());
        if (brokerService == null) {
            throw new ResourceNotFoundException("Broker", request.getBrokerName());
        }

        // Call the broker service to establish connection
        Map<String, Object> result = brokerService.connect(request.getUserId());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Connected to " + request.getBrokerName(),
                "data", result,
                "timestamp", LocalDateTime.now()));
    }

    /**
     * Get broker account status
     * GET /api/brokers/{brokerName}/status/{userId}
     */
    @GetMapping("/{brokerName}/status/{userId}")
    public ResponseEntity<?> getBrokerStatus(
            @PathVariable String brokerName,
            @PathVariable String userId) {

        BrokerService brokerService = brokerServices.get(brokerName.toLowerCase());
        if (brokerService == null) {
            throw new ResourceNotFoundException("Broker", brokerName);
        }

        boolean isConnected = brokerService.isConnected(userId);
        Map<String, Object> status = Map.of(
                "broker", brokerName,
                "connected", isConnected,
                "userId", userId,
                "timestamp", LocalDateTime.now());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", status));
    }

    /**
     * Get holdings from a specific broker
     * GET /api/brokers/{brokerName}/holdings/{userId}
     */
    @GetMapping("/{brokerName}/holdings/{userId}")
    public ResponseEntity<?> getBrokerHoldings(
            @PathVariable String brokerName,
            @PathVariable String userId) {

        BrokerService brokerService = brokerServices.get(brokerName.toLowerCase());
        if (brokerService == null) {
            throw new ResourceNotFoundException("Broker", brokerName);
        }

        List<Map<String, Object>> holdings = brokerService.fetchHoldings(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", holdings,
                "count", holdings.size(),
                "broker", brokerName,
                "timestamp", LocalDateTime.now()));
    }

    /**
     * Get positions from a specific broker
     * GET /api/brokers/{brokerName}/positions/{userId}
     */
    @GetMapping("/{brokerName}/positions/{userId}")
    public ResponseEntity<?> getBrokerPositions(
            @PathVariable String brokerName,
            @PathVariable String userId) {

        BrokerService brokerService = brokerServices.get(brokerName.toLowerCase());
        if (brokerService == null) {
            throw new ResourceNotFoundException("Broker", brokerName);
        }

        List<Map<String, Object>> positions = brokerService.fetchPositions(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", positions,
                "count", positions.size(),
                "broker", brokerName,
                "timestamp", LocalDateTime.now()));
    }

    /**
     * Get orders from a specific broker
     * GET /api/brokers/{brokerName}/orders/{userId}
     */
    @GetMapping("/{brokerName}/orders/{userId}")
    public ResponseEntity<?> getBrokerOrders(
            @PathVariable String brokerName,
            @PathVariable String userId) {

        BrokerService brokerService = brokerServices.get(brokerName.toLowerCase());
        if (brokerService == null) {
            throw new ResourceNotFoundException("Broker", brokerName);
        }

        List<Map<String, Object>> orders = brokerService.fetchOrders(userId);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", orders,
                "count", orders.size(),
                "broker", brokerName,
                "timestamp", LocalDateTime.now()));
    }

    /**
     * Get all available brokers
     * GET /api/brokers/available
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableBrokers() {
        List<String> availableBrokers = brokerServices.keySet().stream()
                .map(String::toUpperCase)
                .sorted()
                .toList();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", availableBrokers,
                "count", availableBrokers.size(),
                "timestamp", LocalDateTime.now()));
    }

    /**
     * Test broker connection
     * GET /api/brokers/{brokerName}/test/{userId}
     */
    @GetMapping("/{brokerName}/test/{userId}")
    public ResponseEntity<?> testBrokerConnection(
            @PathVariable String brokerName,
            @PathVariable String userId) {

        BrokerService brokerService = brokerServices.get(brokerName.toLowerCase());
        if (brokerService == null) {
            throw new ResourceNotFoundException("Broker", brokerName);
        }

        boolean isConnected = brokerService.isConnected(userId);
        Map<String, Object> brokerConfig = brokerService.getBrokerConfig(userId);

        Map<String, Object> testResult = Map.of(
                "broker", brokerName,
                "connected", isConnected,
                "userId", userId,
                "config", brokerConfig,
                "timestamp", LocalDateTime.now());

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", testResult));
    }
}