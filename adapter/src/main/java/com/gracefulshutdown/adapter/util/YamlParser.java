package com.gracefulshutdown.adapter.util;

import com.gracefulshutdown.adapter.domain.Response;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for parsing YAML content into domain objects.
 */
public class YamlParser {
    private static final Logger logger = LoggerFactory.getLogger(YamlParser.class);
    
    private final YAMLMapper yamlMapper;
    
    public YamlParser() {
        this.yamlMapper = new YAMLMapper();
        logger.debug("YamlParser initialized");
    }
    
    /**
     * Parse YAML string to Response object.
     */
    public Response parseYamlToResponse(String yamlContent) throws Exception {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("YAML content cannot be empty");
        }
        
        logger.debug("Parsing YAML to Response object");
        try {
            Response response = yamlMapper.readValue(yamlContent, Response.class);
            logger.debug("Parsed response: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Error parsing YAML to Response: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Parse generic YAML string to specified class.
     */
    public <T> T parseYaml(String yamlContent, Class<T> targetClass) throws Exception {
        if (yamlContent == null || yamlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("YAML content cannot be empty");
        }
        
        logger.debug("Parsing YAML to {}", targetClass.getSimpleName());
        try {
            T obj = yamlMapper.readValue(yamlContent, targetClass);
            logger.debug("Parsed object of type: {}", targetClass.getSimpleName());
            return obj;
        } catch (Exception e) {
            logger.error("Error parsing YAML to {}: {}", targetClass.getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }
}
