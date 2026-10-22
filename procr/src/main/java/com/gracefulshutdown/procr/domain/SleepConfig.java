package com.gracefulshutdown.procr.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SleepConfig represents the sleep timing configuration for PROCR
 * Used to store and transmit sleep settings between Central and PROCR
 */
public class SleepConfig {
    @JsonProperty("fixed_ms")
    private long fixedMs;

    @JsonProperty("random_min_ms")
    private long randomMinMs;

    @JsonProperty("random_step_ms")
    private long randomStepMs;

    @JsonProperty("random_max_ms")
    private long randomMaxMs;

    public SleepConfig() {}

    public SleepConfig(long fixedMs, long randomMinMs, long randomStepMs, long randomMaxMs) {
        this.fixedMs = fixedMs;
        this.randomMinMs = randomMinMs;
        this.randomStepMs = randomStepMs;
        this.randomMaxMs = randomMaxMs;
    }

    public long getFixedMs() {
        return fixedMs;
    }

    public void setFixedMs(long fixedMs) {
        this.fixedMs = fixedMs;
    }

    public long getRandomMinMs() {
        return randomMinMs;
    }

    public void setRandomMinMs(long randomMinMs) {
        this.randomMinMs = randomMinMs;
    }

    public long getRandomStepMs() {
        return randomStepMs;
    }

    public void setRandomStepMs(long randomStepMs) {
        this.randomStepMs = randomStepMs;
    }

    public long getRandomMaxMs() {
        return randomMaxMs;
    }

    public void setRandomMaxMs(long randomMaxMs) {
        this.randomMaxMs = randomMaxMs;
    }

    @Override
    public String toString() {
        return "SleepConfig{" +
                "fixedMs=" + fixedMs +
                ", randomMinMs=" + randomMinMs +
                ", randomStepMs=" + randomStepMs +
                ", randomMaxMs=" + randomMaxMs +
                '}';
    }
}
