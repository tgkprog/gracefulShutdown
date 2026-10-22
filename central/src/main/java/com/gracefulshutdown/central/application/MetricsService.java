package com.gracefulshutdown.central.application;

import com.gracefulshutdown.central.domain.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * MetricsService tracks and aggregates Central server metrics.
 * Captures metrics at 1-minute intervals and rolls up into time buckets.
 */
@Service
public class MetricsService {
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    private final MetricsRepository metricsRepository;
    
    // Current metrics accumulation
    private long minutesConnected = 0;
    private long minutesSessionActive = 0;
    private long txnsSent = 0;
    private long txnsReceived = 0;
    private long txnsApproved = 0;
    private long txnsRejected = 0;
    private long txnsErrored = 0;
    private long totalResponseTimeMs = 0;
    private long peakResponseTimeMs = 0;
    private long responseTimeCount = 0;
    
    private Instant lastMetricsCapture = Instant.now();
    private Instant connectionStartTime = null;
    private Instant sessionStartTime = null;
    private boolean adapterConnected = false;
    private boolean sessionActive = false;
    
    public MetricsService(MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }
    
    /**
     * Called when adapter connects
     */
    public synchronized void onAdapterConnect() {
        adapterConnected = true;
        connectionStartTime = Instant.now();
        logger.info("Metrics: Adapter connected");
    }
    
    /**
     * Called when adapter disconnects
     */
    public synchronized void onAdapterDisconnect() {
        adapterConnected = false;
        if (connectionStartTime != null) {
            long minutes = ChronoUnit.MINUTES.between(connectionStartTime, Instant.now());
            minutesConnected += minutes;
            logger.info("Metrics: Adapter disconnected. Added {} minutes of connection time", minutes);
        }
    }
    
    /**
     * Called when session starts
     */
    public synchronized void onSessionStart() {
        sessionActive = true;
        sessionStartTime = Instant.now();
        logger.info("Metrics: Session started");
    }
    
    /**
     * Called when session ends
     */
    public synchronized void onSessionEnd() {
        sessionActive = false;
        if (sessionStartTime != null) {
            long minutes = ChronoUnit.MINUTES.between(sessionStartTime, Instant.now());
            minutesSessionActive += minutes;
            logger.info("Metrics: Session ended. Added {} minutes of session time", minutes);
        }
    }
    
    /**
     * Record transaction sent
     */
    public synchronized void recordTransactionSent() {
        txnsSent++;
    }
    
    /**
     * Record transaction received
     */
    public synchronized void recordTransactionReceived() {
        txnsReceived++;
    }
    
    /**
     * Record transaction result
     */
    public synchronized void recordTransactionResult(String result, long responseTimeMs) {
        if ("APPROVED".equals(result)) {
            txnsApproved++;
        } else if ("REJECTED".equals(result)) {
            txnsRejected++;
        } else if ("ERROR".equals(result)) {
            txnsErrored++;
        }
        
        // Track response time
        if (responseTimeMs > 0) {
            totalResponseTimeMs += responseTimeMs;
            responseTimeCount++;
            if (responseTimeMs > peakResponseTimeMs) {
                peakResponseTimeMs = responseTimeMs;
            }
        }
    }
    
    /**
     * Capture metrics at 1-minute intervals and store in database.
     * Run every minute (60000 ms).
     */
    @Scheduled(fixedDelay = 60000)
    public synchronized void captureMetrics() {
        try {
            Instant now = Instant.now();
            
            // Add current connection and session time if still active
            if (adapterConnected && connectionStartTime != null) {
                long minutes = ChronoUnit.MINUTES.between(connectionStartTime, now);
                minutesConnected += minutes;
                connectionStartTime = now;
            }
            
            if (sessionActive && sessionStartTime != null) {
                long minutes = ChronoUnit.MINUTES.between(sessionStartTime, now);
                minutesSessionActive += minutes;
                sessionStartTime = now;
            }
            
            // Create metrics entry for this minute
            Metrics metrics = new Metrics(now, "MINUTE");
            metrics.setMinutesConnected((int) minutesConnected);
            metrics.setMinutesSessionActive((int) minutesSessionActive);
            metrics.setTxnsSent(txnsSent);
            metrics.setTxnsReceived(txnsReceived);
            metrics.setTxnsApproved(txnsApproved);
            metrics.setTxnsRejected(txnsRejected);
            metrics.setTxnsErrored(txnsErrored);
            
            if (responseTimeCount > 0) {
                metrics.setAvgResponseTimeMs(totalResponseTimeMs / responseTimeCount);
            }
            metrics.setPeakResponseTimeMs(peakResponseTimeMs);
            
            metrics.calculatePercentages();
            metricsRepository.save(metrics);
            
            logger.debug("Captured metrics: sent={}, received={}, approved={}, rejected={}, error={}",
                txnsSent, txnsReceived, txnsApproved, txnsRejected, txnsErrored);
            
            // Reset for next minute
            lastMetricsCapture = now;
            totalResponseTimeMs = 0;
            responseTimeCount = 0;
            peakResponseTimeMs = 0;
            
        } catch (Exception e) {
            logger.error("Error capturing metrics: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Rollup minute metrics into 5-hour chunks.
     * Run every 5 hours.
     */
    @Scheduled(fixedDelay = 18000000)  // 5 hours
    public synchronized void rollup5Hour() {
        try {
            Instant now = Instant.now();
            Instant fiveHoursAgo = now.minus(5, ChronoUnit.HOURS);
            
            List<Metrics> minuteMetrics = metricsRepository.findByTimestampBetweenAndBucketTypeOrderByTimestampDesc(
                fiveHoursAgo, now, "MINUTE"
            );
            
            if (minuteMetrics.isEmpty()) {
                return;
            }
            
            Metrics rollup = aggregateMetrics(minuteMetrics, now, "FIVE_HOUR");
            metricsRepository.save(rollup);
            
            logger.info("Rolled up 5-hour metrics: {} minute records aggregated", minuteMetrics.size());
        } catch (Exception e) {
            logger.error("Error in 5-hour rollup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Rollup into 12-hour chunks.
     * Run every 12 hours.
     */
    @Scheduled(fixedDelay = 43200000)  // 12 hours
    public synchronized void rollup12Hour() {
        try {
            Instant now = Instant.now();
            Instant twelveHoursAgo = now.minus(12, ChronoUnit.HOURS);
            
            List<Metrics> allMetrics = metricsRepository.findByTimestampBetweenAndBucketTypeOrderByTimestampDesc(
                twelveHoursAgo, now, "FIVE_HOUR"
            );
            
            if (allMetrics.isEmpty()) {
                return;
            }
            
            Metrics rollup = aggregateMetrics(allMetrics, now, "TWELVE_HOUR");
            metricsRepository.save(rollup);
            
            logger.info("Rolled up 12-hour metrics: {} records aggregated", allMetrics.size());
        } catch (Exception e) {
            logger.error("Error in 12-hour rollup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Rollup into daily chunks.
     * Run daily at midnight.
     */
    @Scheduled(fixedDelay = 86400000)  // 24 hours
    public synchronized void rollupDaily() {
        try {
            Instant now = Instant.now();
            Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);
            
            List<Metrics> allMetrics = metricsRepository.findByTimestampBetweenAndBucketTypeOrderByTimestampDesc(
                oneDayAgo, now, "TWELVE_HOUR"
            );
            
            if (allMetrics.isEmpty()) {
                return;
            }
            
            Metrics rollup = aggregateMetrics(allMetrics, now, "DAILY");
            metricsRepository.save(rollup);
            
            logger.info("Rolled up daily metrics: {} records aggregated", allMetrics.size());
        } catch (Exception e) {
            logger.error("Error in daily rollup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Aggregate multiple metrics into a single rollup record
     */
    private Metrics aggregateMetrics(List<Metrics> metricsList, Instant timestamp, String bucketType) {
        Metrics rollup = new Metrics(timestamp, bucketType);
        
        long totalMinutesConnected = 0;
        long totalMinutesSessionActive = 0;
        long totalTxnsSent = 0;
        long totalTxnsReceived = 0;
        long totalTxnsApproved = 0;
        long totalTxnsRejected = 0;
        long totalTxnsErrored = 0;
        long maxPeakResponseTime = 0;
        long totalResponseTime = 0;
        long responseTimeCount = 0;
        
        for (Metrics m : metricsList) {
            totalMinutesConnected += m.getMinutesConnected();
            totalMinutesSessionActive += m.getMinutesSessionActive();
            totalTxnsSent += m.getTxnsSent();
            totalTxnsReceived += m.getTxnsReceived();
            totalTxnsApproved += m.getTxnsApproved();
            totalTxnsRejected += m.getTxnsRejected();
            totalTxnsErrored += m.getTxnsErrored();
            
            if (m.getPeakResponseTimeMs() > maxPeakResponseTime) {
                maxPeakResponseTime = m.getPeakResponseTimeMs();
            }
            
            if (m.getAvgResponseTimeMs() > 0) {
                totalResponseTime += m.getAvgResponseTimeMs();
                responseTimeCount++;
            }
        }
        
        rollup.setMinutesConnected((int) totalMinutesConnected);
        rollup.setMinutesSessionActive((int) totalMinutesSessionActive);
        rollup.setTxnsSent(totalTxnsSent);
        rollup.setTxnsReceived(totalTxnsReceived);
        rollup.setTxnsApproved(totalTxnsApproved);
        rollup.setTxnsRejected(totalTxnsRejected);
        rollup.setTxnsErrored(totalTxnsErrored);
        rollup.setPeakResponseTimeMs(maxPeakResponseTime);
        
        if (responseTimeCount > 0) {
            rollup.setAvgResponseTimeMs(totalResponseTime / responseTimeCount);
        }
        
        rollup.calculatePercentages();
        return rollup;
    }
    
    /**
     * Get status for last 1 hour
     */
    public synchronized Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        status.put("timestamp", Instant.now());
        status.put("minutesConnected", minutesConnected);
        status.put("minutesSessionActive", minutesSessionActive);
        status.put("adapterConnected", adapterConnected);
        status.put("sessionActive", sessionActive);
        
        status.put("txnsSent", txnsSent);
        status.put("txnsReceived", txnsReceived);
        status.put("txnsApproved", txnsApproved);
        status.put("txnsRejected", txnsRejected);
        status.put("txnsErrored", txnsErrored);
        
        long total = txnsApproved + txnsRejected + txnsErrored;
        if (total > 0) {
            status.put("approvalPercentage", (100.0 * txnsApproved) / total);
            status.put("rejectionPercentage", (100.0 * txnsRejected) / total);
            status.put("errorPercentage", (100.0 * txnsErrored) / total);
        } else {
            status.put("approvalPercentage", 0.0);
            status.put("rejectionPercentage", 0.0);
            status.put("errorPercentage", 0.0);
        }
        
        if (responseTimeCount > 0) {
            status.put("avgResponseTimeMs", totalResponseTimeMs / responseTimeCount);
        } else {
            status.put("avgResponseTimeMs", 0L);
        }
        status.put("peakResponseTimeMs", peakResponseTimeMs);
        
        return status;
    }
    
    /**
     * Get history for different time buckets
     */
    public Map<String, Object> getHistory() {
        Map<String, Object> history = new HashMap<>();
        
        history.put("oneHour", metricsRepository.findByBucketTypeOrderByTimestampDesc("MINUTE"));
        history.put("fiveHour", metricsRepository.findByBucketTypeOrderByTimestampDesc("FIVE_HOUR"));
        history.put("twelveHour", metricsRepository.findByBucketTypeOrderByTimestampDesc("TWELVE_HOUR"));
        history.put("daily", metricsRepository.findByBucketTypeOrderByTimestampDesc("DAILY"));
        
        return history;
    }
}
