package com.gracefulshutdown.procr.controller;

import com.gracefulshutdown.procr.application.SleepConfigService;
import com.gracefulshutdown.procr.domain.SleepConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * SleepConfigController exposes APIs to get and update sleep configuration
 * Allows runtime modification of sleep timings without server restart
 * 
 * Endpoints:
 * GET /config/sleep - Get current sleep configuration
 * POST /config/sleep - Update sleep configuration
 */
@RestController
@RequestMapping("/config")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SleepConfigController {
    private static final Logger logger = LoggerFactory.getLogger(SleepConfigController.class);

    private final SleepConfigService sleepConfigService;

    public SleepConfigController(SleepConfigService sleepConfigService) {
        this.sleepConfigService = sleepConfigService;
    }

    /**
     * GET /config/sleep - Returns current sleep configuration
     */
    @GetMapping(value = "/sleep", produces = MediaType.APPLICATION_JSON_VALUE)
    public SleepConfig getSleepConfig() {
        logger.info("GET /config/sleep - Retrieving sleep configuration");
        SleepConfig config = sleepConfigService.getSleepConfig();
        logger.info("Returning sleep config: {}", config);
        return config;
    }

    /**
     * POST /config/sleep - Updates sleep configuration
     * Request body: SleepConfig JSON with fields:
     *   - fixed_ms: Fixed delay in milliseconds
     *   - random_min_ms: Minimum random value in milliseconds
     *   - random_step_ms: Step increment for random values
     *   - random_max_ms: Maximum random value in milliseconds
     */
    @PostMapping(value = "/sleep", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public SleepConfig updateSleepConfig(@RequestBody SleepConfig config) {
        logger.info("POST /config/sleep - Updating sleep configuration: {}", config);
        SleepConfig updated = sleepConfigService.updateSleepConfig(config);
        logger.info("Sleep configuration updated successfully: {}", updated);
        return updated;
    }
}
