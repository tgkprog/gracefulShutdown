package com.gracefulshutdown.central.config;

import com.gracefulshutdown.central.network.TransactionCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CentralServerConfig {

    @Bean
    public TransactionCache transactionCache(@Value("${txn.timeout.seconds:9}") int timeoutSeconds) {
        return new TransactionCache(timeoutSeconds * 1000L);
    }
}
