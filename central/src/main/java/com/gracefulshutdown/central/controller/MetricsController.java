package com.gracefulshutdown.central.controller;

import com.gracefulshutdown.central.application.MetricsService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for metrics and status dashboard.
 * Serves JSON API and HTML dashboard.
 */
@Controller
@RequestMapping("/central")
public class MetricsController {
    
    private final MetricsService metricsService;
    
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    /**
     * Get current status (JSON API)
     */
    @GetMapping(value = "/api/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getStatus() {
        return metricsService.getStatus();
    }
    
    /**
     * Get historical metrics (JSON API)
     */
    @GetMapping(value = "/api/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getHistory() {
        return metricsService.getHistory();
    }
    
    /**
     * Get both status and history (JSON API)
     */
    @GetMapping(value = "/api/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> getMetrics() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", metricsService.getStatus());
        response.put("history", metricsService.getHistory());
        return response;
    }
    
    /**
     * Serve HTML dashboard
     */
    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_HTML_VALUE)
    public String getDashboard() {
        return "dashboard";
    }
}
