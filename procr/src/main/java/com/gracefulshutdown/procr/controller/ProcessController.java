package com.gracefulshutdown.procr.controller;

import com.gracefulshutdown.procr.application.TransactionProcessor;
import com.gracefulshutdown.procr.domain.Response;
import com.gracefulshutdown.procr.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * ProcessController handles transaction processing endpoints
 * Accepts YAML request, returns YAML response
 */
@RestController
public class ProcessController {
    private static final Logger logger = LoggerFactory.getLogger(ProcessController.class);

    private final TransactionProcessor transactionProcessor;

    public ProcessController(TransactionProcessor transactionProcessor) {
        this.transactionProcessor = transactionProcessor;
    }

    @PostMapping(value = "/process", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public Response process(@RequestBody Transaction transaction) throws InterruptedException {
        logger.info("Received process request for transaction: {}", transaction.getTxn_id());

        Response response = transactionProcessor.process(transaction);

        logger.info("Sending process response: {}", response.getTxn_id());

        return response;
    }
}
