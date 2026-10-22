package com.gracefulshutdown.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * AdapterConfig provides Spring beans for Adapter service
 */
@Configuration
public class AdapterConfig {

    /**
     * Create RestTemplate bean with timeout configuration
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .interceptors((request, body, execution) -> execution.execute(request, body))
                .build();
    }

    /**
     * Create JSON ObjectMapper bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
