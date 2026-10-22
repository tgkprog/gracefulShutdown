package com.gracefulshutdown.adapter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gracefulshutdown.adapter.domain.Response;
import com.gracefulshutdown.adapter.domain.SessionMessage;
import com.gracefulshutdown.adapter.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * JsonYamlConverterImpl provides bidirectional JSON/YAML conversion
 */
@Service
public class JsonYamlConverterImpl implements JsonYamlConverter {
    private static final Logger logger = LoggerFactory.getLogger(JsonYamlConverterImpl.class);

    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public JsonYamlConverterImpl() {
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    @Override
    public String transactionToYaml(Transaction transaction) throws Exception {
        logger.debug("Converting Transaction to YAML: id={}", transaction.getId());

        // First convert to JSON representation, then to YAML
        String yamlContent = yamlMapper.writeValueAsString(transaction);

        logger.debug("Converted Transaction to YAML (length={})", yamlContent.length());

        return yamlContent;
    }

    @Override
    public Response yamlToResponse(String yamlContent) throws Exception {
        logger.debug("Converting YAML to Response (length={})", yamlContent.length());

        Response response = yamlMapper.readValue(yamlContent, Response.class);

        logger.debug("Converted YAML to Response: id={}, type={}", 
                   response.getTxn_id(), response.getResponse_type());

        return response;
    }

    @Override
    public String sessionMessageToYaml(SessionMessage message) throws Exception {
        logger.debug("Converting SessionMessage to YAML: type={}", message.getMessage_type());

        String yamlContent = yamlMapper.writeValueAsString(message);

        logger.debug("Converted SessionMessage to YAML (length={})", yamlContent.length());

        return yamlContent;
    }

    @Override
    public String jsonToYaml(String jsonContent) throws Exception {
        logger.debug("Converting JSON to YAML (input length={})", jsonContent.length());
        
        try {
            // Parse JSON
            Object jsonObj = jsonMapper.readValue(jsonContent, Object.class);
            
            // Write as YAML
            String yamlContent = yamlMapper.writeValueAsString(jsonObj);
            
            logger.debug("Converted JSON to YAML (output length={})", yamlContent.length());
            
            return yamlContent;
        } catch (Exception e) {
            logger.error("Error converting JSON to YAML: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String yamlToJson(String yamlContent) throws Exception {
        logger.debug("Converting YAML to JSON (input length={})", yamlContent.length());
        
        try {
            // Parse YAML
            Object yamlObj = yamlMapper.readValue(yamlContent, Object.class);
            
            // Write as JSON
            String jsonContent = jsonMapper.writeValueAsString(yamlObj);
            
            logger.debug("Converted YAML to JSON (output length={})", jsonContent.length());
            
            return jsonContent;
        } catch (Exception e) {
            logger.error("Error converting YAML to JSON: {}", e.getMessage(), e);
            throw e;
        }
    }
}
