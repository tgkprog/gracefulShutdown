package com.gracefulshutdown.procr.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HealthController provides health check endpoint
 * Returns YAML response
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class HealthController {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> health() {
        logger.info("Health check requested");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "PROCR");
        response.put("timestamp", System.currentTimeMillis());

        logger.info("Health check response: {}", response);

        return response;
    }
}
