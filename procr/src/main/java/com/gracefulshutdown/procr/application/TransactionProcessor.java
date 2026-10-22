package com.gracefulshutdown.procr.application;

import com.gracefulshutdown.procr.domain.Response;
import com.gracefulshutdown.procr.domain.RejectionReason;
import com.gracefulshutdown.procr.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * TransactionProcessor orchestrates the complete transaction processing flow:
 * 1. Receive YAML transaction
 * 2. Log transaction received
 * 3. Determine approval/rejection
 * 4. Log decision
 * 5. Determine if sleep needed and calculate delay
 * 6. Log calculated delay
 * 7. Sleep if needed
 * 8. Log sleep completion
 * 9. Generate response
 * 10. Return response
 */
@Service
public class TransactionProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);

    private final ApprovalEngine approvalEngine;
    private final ResponseDelayCalculator delayCalculator;

    public TransactionProcessor(ApprovalEngine approvalEngine, ResponseDelayCalculator delayCalculator) {
        this.approvalEngine = approvalEngine;
        this.delayCalculator = delayCalculator;
    }

    public Response process(Transaction transaction) throws InterruptedException {
        // Step 1-2: Receive and log transaction
        logger.info("Step 1-2: Received transaction: txn_id={}, type={}, total={}", 
                   transaction.getTxn_id(), transaction.getType(), transaction.getTotal());

        // Step 3-4: Determine approval and log decision
        boolean approved = approvalEngine.decideApproval();
        String decisionType = approved ? "APPROVE" : "REJECT";
        logger.info("Step 3-4: Decision made: {}", decisionType);

        // Step 5-6: Calculate delay and log it
        long delayMs = delayCalculator.calculateDelay();
        logger.info("Step 5-6: Calculated delay: {} ms", delayMs);

        // Step 7-8: Sleep if delay > 0 and log completion
        if (delayMs > 0) {
            logger.info("Step 7: Starting sleep: {} ms", delayMs);
            Thread.sleep(delayMs);
            logger.info("Step 8: Sleep completed after {} ms", delayMs);
        } else {
            logger.info("Step 7-8: No delay needed, skipping sleep");
        }

        // Step 9-10: Generate and return response
        Response response = buildResponse(transaction, approved);
        logger.info("Step 9-10: Generated response: type={}, status={}", 
                   response.getResponse_type(), response.getStatus());

        return response;
    }

    private Response buildResponse(Transaction transaction, boolean approved) {
        String responseType = approved ? "APPROVE" : "REJECT";
        String status = approved ? "SUCCESS" : "FAILED";
        String message;

        if (approved) {
            message = "Transaction processed successfully";
        } else {
            RejectionReason reason = approvalEngine.selectRejectionReason();
            message = "Transaction rejected: " + reason.getMessage();
        }

        return new Response(
                responseType,
                transaction.getTxn_id(),
                status,
                message,
                Instant.now().toString()
        );
    }
}
