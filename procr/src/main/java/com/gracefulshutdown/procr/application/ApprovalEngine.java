package com.gracefulshutdown.procr.application;

import com.gracefulshutdown.procr.domain.RejectionReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * ApprovalEngine decides whether to approve or reject a transaction
 * Logic: 99% APPROVE, 1% REJECT
 */
@Service
public class ApprovalEngine {
    private static final Logger logger = LoggerFactory.getLogger(ApprovalEngine.class);
    private final Random random = new Random();

    public boolean decideApproval() {
        int randomValue = random.nextInt(100) + 1;  // 1-100
        boolean approved = randomValue <= 99;  // Production: 1-99 approve, 100 reject (1%)
        
        logger.debug("Generated random: {} -> {}", randomValue, approved ? "APPROVE" : "REJECT");
        
        return approved;
    }

    public RejectionReason selectRejectionReason() {
        RejectionReason[] reasons = RejectionReason.values();
        RejectionReason selected = reasons[random.nextInt(reasons.length)];
        
        logger.debug("Selected rejection reason: {}", selected.getMessage());
        
        return selected;
    }
}
