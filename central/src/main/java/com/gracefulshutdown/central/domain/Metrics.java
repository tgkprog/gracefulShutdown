package com.gracefulshutdown.central.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Metrics entity for storing Central server status and transaction metrics.
 * Captured at 1-minute intervals and aggregated into time buckets.
 */
@Entity
@Table(name = "metrics", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_bucket", columnList = "bucket_type"),
    @Index(name = "idx_captured_at", columnList = "captured_at")
})
public class Metrics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Timestamp when metrics were captured (1-minute resolution)
     */
    @Column(nullable = false)
    private Instant timestamp;
    
    /**
     * When this metric entry was recorded in database
     */
    @Column(nullable = false)
    private Instant capturedAt;
    
    /**
     * Bucket type: MINUTE (raw), HOURLY (rollup), DAILY (rollup)
     */
    @Column(nullable = false)
    private String bucketType;  // MINUTE, FIVE_HOUR, TWELVE_HOUR, DAILY
    
    /**
     * Minutes adapter connection was active during this period
     */
    @Column(nullable = false)
    private Integer minutesConnected = 0;
    
    /**
     * Minutes session was active during this period
     */
    @Column(nullable = false)
    private Integer minutesSessionActive = 0;
    
    /**
     * Total transactions sent
     */
    @Column(nullable = false)
    private Long txnsSent = 0L;
    
    /**
     * Total transactions received
     */
    @Column(nullable = false)
    private Long txnsReceived = 0L;
    
    /**
     * Approved transactions
     */
    @Column(nullable = false)
    private Long txnsApproved = 0L;
    
    /**
     * Rejected transactions
     */
    @Column(nullable = false)
    private Long txnsRejected = 0L;
    
    /**
     * Errored transactions
     */
    @Column(nullable = false)
    private Long txnsErrored = 0L;
    
    /**
     * Percentage of transactions approved (calculated)
     */
    @Column(nullable = false)
    private Double approvalPercentage = 0.0;
    
    /**
     * Percentage of transactions rejected (calculated)
     */
    @Column(nullable = false)
    private Double rejectionPercentage = 0.0;
    
    /**
     * Percentage of transactions errored (calculated)
     */
    @Column(nullable = false)
    private Double errorPercentage = 0.0;
    
    /**
     * Average response time in milliseconds
     */
    @Column(nullable = false)
    private Long avgResponseTimeMs = 0L;
    
    /**
     * Peak (max) response time in milliseconds
     */
    @Column(nullable = false)
    private Long peakResponseTimeMs = 0L;
    
    // Constructors
    public Metrics() {
    }
    
    public Metrics(Instant timestamp, String bucketType) {
        this.timestamp = timestamp;
        this.capturedAt = Instant.now();
        this.bucketType = bucketType;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Instant getCapturedAt() {
        return capturedAt;
    }
    
    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
    }
    
    public String getBucketType() {
        return bucketType;
    }
    
    public void setBucketType(String bucketType) {
        this.bucketType = bucketType;
    }
    
    public Integer getMinutesConnected() {
        return minutesConnected;
    }
    
    public void setMinutesConnected(Integer minutesConnected) {
        this.minutesConnected = minutesConnected;
    }
    
    public Integer getMinutesSessionActive() {
        return minutesSessionActive;
    }
    
    public void setMinutesSessionActive(Integer minutesSessionActive) {
        this.minutesSessionActive = minutesSessionActive;
    }
    
    public Long getTxnsSent() {
        return txnsSent;
    }
    
    public void setTxnsSent(Long txnsSent) {
        this.txnsSent = txnsSent;
    }
    
    public Long getTxnsReceived() {
        return txnsReceived;
    }
    
    public void setTxnsReceived(Long txnsReceived) {
        this.txnsReceived = txnsReceived;
    }
    
    public Long getTxnsApproved() {
        return txnsApproved;
    }
    
    public void setTxnsApproved(Long txnsApproved) {
        this.txnsApproved = txnsApproved;
    }
    
    public Long getTxnsRejected() {
        return txnsRejected;
    }
    
    public void setTxnsRejected(Long txnsRejected) {
        this.txnsRejected = txnsRejected;
    }
    
    public Long getTxnsErrored() {
        return txnsErrored;
    }
    
    public void setTxnsErrored(Long txnsErrored) {
        this.txnsErrored = txnsErrored;
    }
    
    public Double getApprovalPercentage() {
        return approvalPercentage;
    }
    
    public void setApprovalPercentage(Double approvalPercentage) {
        this.approvalPercentage = approvalPercentage;
    }
    
    public Double getRejectionPercentage() {
        return rejectionPercentage;
    }
    
    public void setRejectionPercentage(Double rejectionPercentage) {
        this.rejectionPercentage = rejectionPercentage;
    }
    
    public Double getErrorPercentage() {
        return errorPercentage;
    }
    
    public void setErrorPercentage(Double errorPercentage) {
        this.errorPercentage = errorPercentage;
    }
    
    public Long getAvgResponseTimeMs() {
        return avgResponseTimeMs;
    }
    
    public void setAvgResponseTimeMs(Long avgResponseTimeMs) {
        this.avgResponseTimeMs = avgResponseTimeMs;
    }
    
    public Long getPeakResponseTimeMs() {
        return peakResponseTimeMs;
    }
    
    public void setPeakResponseTimeMs(Long peakResponseTimeMs) {
        this.peakResponseTimeMs = peakResponseTimeMs;
    }
    
    /**
     * Calculate percentages from transaction counts
     */
    public void calculatePercentages() {
        long total = txnsApproved + txnsRejected + txnsErrored;
        if (total > 0) {
            this.approvalPercentage = (100.0 * txnsApproved) / total;
            this.rejectionPercentage = (100.0 * txnsRejected) / total;
            this.errorPercentage = (100.0 * txnsErrored) / total;
        }
    }
}
