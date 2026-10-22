package com.gracefulshutdown.central.application;

import com.gracefulshutdown.central.domain.Metrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Metrics data access.
 */
@Repository
public interface MetricsRepository extends JpaRepository<Metrics, Long> {
    
    /**
     * Find metrics by timestamp and bucket type
     */
    Optional<Metrics> findByTimestampAndBucketType(Instant timestamp, String bucketType);
    
    /**
     * Find all metrics for given bucket type ordered by timestamp descending
     */
    List<Metrics> findByBucketTypeOrderByTimestampDesc(String bucketType);
    
    /**
     * Find metrics within time range
     */
    List<Metrics> findByTimestampBetweenAndBucketTypeOrderByTimestampDesc(
        Instant startTime, 
        Instant endTime, 
        String bucketType
    );
    
    /**
     * Find latest metric for bucket type
     */
    @Query("SELECT m FROM Metrics m WHERE m.bucketType = :bucketType ORDER BY m.timestamp DESC LIMIT 1")
    Optional<Metrics> findLatestByBucketType(@Param("bucketType") String bucketType);
    
    /**
     * Find all metrics older than given timestamp (for cleanup)
     */
    List<Metrics> findByTimestampBeforeOrderByTimestampDesc(Instant timestamp);
    
    /**
     * Count metrics by bucket type
     */
    Long countByBucketType(String bucketType);
}
