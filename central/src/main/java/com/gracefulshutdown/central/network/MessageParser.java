package com.gracefulshutdown.central.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gracefulshutdown.central.domain.Acknowledgment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MessageParser {
    private static final Logger logger = LoggerFactory.getLogger(MessageParser.class);
    private final ObjectMapper yamlMapper;

    public MessageParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public enum MessageType {
        SESSION_START, SESSION_END, TXN_ACKNOWLEDGE, UNKNOWN
    }

    public MessageType parseMessageType(String yaml) throws Exception {
        try {
            JsonNode root = yamlMapper.readTree(yaml);
            
            if (root.has("message_type")) {
                String type = root.get("message_type").asText();
                return MessageType.valueOf(type.toUpperCase());
            }
            
            if (root.has("txn_id") && root.has("status")) {
                return MessageType.TXN_ACKNOWLEDGE;
            }
            
            return MessageType.UNKNOWN;
        } catch (Exception e) {
            logger.error("Error parsing message type from YAML", e);
            throw e;
        }
    }

    public Acknowledgment parseAcknowledgment(String yaml) throws Exception {
        try {
            return yamlMapper.readValue(yaml, Acknowledgment.class);
        } catch (Exception e) {
            logger.error("Error parsing acknowledgment from YAML", e);
            throw e;
        }
    }

    public String getMessageField(String yaml, String fieldName) throws Exception {
        JsonNode root = yamlMapper.readTree(yaml);
        if (root.has(fieldName)) {
            return root.get(fieldName).asText();
        }
        return null;
    }
}
