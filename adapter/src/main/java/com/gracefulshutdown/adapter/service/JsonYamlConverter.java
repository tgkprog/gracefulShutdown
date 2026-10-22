package com.gracefulshutdown.adapter.service;

import com.gracefulshutdown.adapter.domain.Response;
import com.gracefulshutdown.adapter.domain.SessionMessage;
import com.gracefulshutdown.adapter.domain.Transaction;

/**
 * JsonYamlConverter interface for bidirectional conversion
 */
public interface JsonYamlConverter {
    
    /**
     * Convert Transaction object to YAML string
     */
    String transactionToYaml(Transaction transaction) throws Exception;
    
    /**
     * Convert YAML string to Response object
     */
    Response yamlToResponse(String yamlContent) throws Exception;
    
    /**
     * Convert SessionMessage object to YAML string
     */
    String sessionMessageToYaml(SessionMessage message) throws Exception;
    
    /**
     * Convert JSON string to YAML string
     */
    String jsonToYaml(String jsonContent) throws Exception;
    
    /**
     * Convert YAML string to JSON string
     */
    String yamlToJson(String yamlContent) throws Exception;
}
