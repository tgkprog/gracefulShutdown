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
    private final ObjectMapper jsonMapper;

    public MessageParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.jsonMapper = new ObjectMapper();
    }

    public enum MessageType {
        SESSION_START, SESSION_END, TXN_ACKNOWLEDGE, UNKNOWN
    }

    public MessageType parseMessageType(String message) throws Exception {
        try {
            JsonNode root = parseMessage(message);
            
            if (root.has("message_type")) {
                String type = root.get("message_type").asText();
                return MessageType.valueOf(type.toUpperCase());
            }
            
            if (root.has("txn_id") && root.has("status")) {
                return MessageType.TXN_ACKNOWLEDGE;
            }
            
            return MessageType.UNKNOWN;
        } catch (Exception e) {
            logger.error("Error parsing message type", e);
            throw e;
        }
    }

    public Acknowledgment parseAcknowledgment(String message) throws Exception {
        try {
            JsonNode root = parseMessage(message);
            Acknowledgment ack = new Acknowledgment();
            
            if (root.has("txn_id")) {
                ack.setTxnId(root.get("txn_id").asText());
            }
            if (root.has("stan")) {
                ack.setStan(root.get("stan").asText());
            }
            if (root.has("response_type")) {
                ack.setResponse_type(root.get("response_type").asText());
            }
            if (root.has("status")) {
                ack.setStatus(root.get("status").asText());
            }
            if (root.has("message")) {
                ack.setReason(root.get("message").asText());
            }
            if (root.has("timestamp")) {
                ack.setTimestamp(Instant.parse(root.get("timestamp").asText()));
            } else {
                ack.setTimestamp(Instant.now());
            }
            
            return ack;
        } catch (Exception e) {
            logger.error("Error parsing acknowledgment", e);
            throw e;
        }
    }

    public String getMessageField(String message, String fieldName) throws Exception {
        JsonNode root = parseMessage(message);
        if (root.has(fieldName)) {
            return root.get(fieldName).asText();
        }
        return null;
    }

    /**
     * Try to parse message as JSON first, then fall back to YAML
     */
    private JsonNode parseMessage(String message) throws Exception {
        message = message.trim();
        
        // Try JSON first (starts with {)
        if (message.startsWith("{")) {
            try {
                return jsonMapper.readTree(message);
            } catch (Exception e) {
                logger.debug("Failed to parse as JSON, trying YAML");
            }
        }
        
        // Try YAML
        return yamlMapper.readTree(message);
    }
}
