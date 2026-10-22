package com.gracefulshutdown.procr.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ResponseDelayCalculator implements the formula: sleep_ms = fixed_delay + random_value
 * Where: random_value is selected from [min_random, min_random+step, ..., max_random]
 * Uses SleepConfigService to read current configuration (can be updated at runtime)
 */
@Service
public class ResponseDelayCalculator {
    private static final Logger logger = LoggerFactory.getLogger(ResponseDelayCalculator.class);
    private final Random random = new Random();
    private final SleepConfigService sleepConfigService;

    public ResponseDelayCalculator(SleepConfigService sleepConfigService) {
        this.sleepConfigService = sleepConfigService;
    }

    public long calculateDelay() {
        long fixedDelay = sleepConfigService.getFixedDelay();
        long minRandom = sleepConfigService.getMinRandom();
        long step = sleepConfigService.getStep();
        long maxRandom = sleepConfigService.getMaxRandom();

        // Generate possible values: [min_random, min_random+step, min_random+2*step, ..., max_random]
        List<Long> possibleValues = new ArrayList<>();
        for (long value = minRandom; value <= maxRandom; value += step) {
            possibleValues.add(value);
        }

        // If only one value, return it
        if (possibleValues.isEmpty()) {
            possibleValues.add(minRandom);
        }

        // Randomly select one value
        long randomValue = possibleValues.get(random.nextInt(possibleValues.size()));
        long totalDelay = fixedDelay + randomValue;

        logger.debug("Delay calculation: fixed={}, min={}, step={}, max={}", fixedDelay, minRandom, step, maxRandom);
        logger.debug("Selected random_value={}", randomValue);
        logger.debug("Total sleep_ms={}", totalDelay);

        return totalDelay;
    }
}
