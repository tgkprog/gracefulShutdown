package com.gracefulshutdown.procr.application;

import com.gracefulshutdown.procr.domain.SleepConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SleepConfigService manages runtime sleep configuration for PROCR
 * Allows getting and updating sleep timings without server restart
 */
@Service
public class SleepConfigService {
    private static final Logger logger = LoggerFactory.getLogger(SleepConfigService.class);

    private volatile long fixedDelay;
    private volatile long minRandom;
    private volatile long step;
    private volatile long maxRandom;

    public SleepConfigService(
            @Value("${procr.response.delay.fixed:0}") long fixedDelay,
            @Value("${procr.response.delay.min_random:0}") long minRandom,
            @Value("${procr.response.delay.step:100}") long step,
            @Value("${procr.response.delay.max_random:200}") long maxRandom) {
        this.fixedDelay = fixedDelay;
        this.minRandom = minRandom;
        this.step = step;
        this.maxRandom = maxRandom;
        logger.info("SleepConfigService initialized: fixed={}, min={}, step={}, max={}",
                fixedDelay, minRandom, step, maxRandom);
    }

    /**
     * Get current sleep configuration
     */
    public SleepConfig getSleepConfig() {
        return new SleepConfig(fixedDelay, minRandom, step, maxRandom);
    }

    /**
     * Update sleep configuration at runtime
     */
    public SleepConfig updateSleepConfig(SleepConfig config) {
        logger.info("Updating sleep config: {}", config);
        this.fixedDelay = config.getFixedMs();
        this.minRandom = config.getRandomMinMs();
        this.step = config.getRandomStepMs();
        this.maxRandom = config.getRandomMaxMs();
        logger.info("Sleep config updated successfully");
        return getSleepConfig();
    }

    public long getFixedDelay() {
        return fixedDelay;
    }

    public long getMinRandom() {
        return minRandom;
    }

    public long getStep() {
        return step;
    }

    public long getMaxRandom() {
        return maxRandom;
    }
}
