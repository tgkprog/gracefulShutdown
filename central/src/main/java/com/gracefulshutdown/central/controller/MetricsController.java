package com.gracefulshutdown.central.controller;

import com.gracefulshutdown.central.application.MetricsService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller for metrics, status, and reporting.
 * Serves comprehensive JSON APIs for monitoring and information service.
 */
@RestController
@RequestMapping("/central")
public class MetricsController {
    
    private final MetricsService metricsService;
    
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping(value = "/api/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "Central Transaction Server");
        response.put("web_port", 8080);
        response.put("tcp_port", 5009);
        return response;
    }
    
    /**
     * Get current status (JSON API)
     */
    @GetMapping(value = "/api/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getStatus() {
        return metricsService.getStatus();
    }
    
    /**
     * Get historical metrics (JSON API)
     */
    @GetMapping(value = "/api/history", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getHistory() {
        return metricsService.getHistory();
    }
    
    /**
     * Get both status and history (JSON API)
     */
    @GetMapping(value = "/api/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", metricsService.getStatus());
        response.put("history", metricsService.getHistory());
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * Get server information and configuration
     */
    @GetMapping(value = "/api/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "Central Transaction Server");
        info.put("version", "1.0.0");
        info.put("ports", Map.of(
            "web_api_port", 8080,
            "tcp_server_port", 5009
        ));
        info.put("description", "Transaction generator and sender with web API for reporting");
        info.put("endpoints", Map.of(
            "health", "/central/api/health",
            "status", "/central/api/status",
            "metrics", "/central/api/metrics",
            "history", "/central/api/history",
            "info", "/central/api/info",
            "dashboard", "/central/dashboard"
        ));
        return info;
    }
    
    /**
     * Serve HTML dashboard
     */
    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String getDashboard() {
        return "<h1>Dashboard - Coming Soon</h1><p>Real-time metrics dashboard will be available here.</p>";
    }
}
