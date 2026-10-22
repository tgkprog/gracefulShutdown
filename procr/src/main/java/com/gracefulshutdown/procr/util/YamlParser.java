package com.gracefulshutdown.procr.util;

import com.gracefulshutdown.procr.domain.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * YamlParser parses YAML request body into Transaction object
 */
@Component
public class YamlParser {
    private static final Logger logger = LoggerFactory.getLogger(YamlParser.class);
    private final ObjectMapper yamlMapper;

    public YamlParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public Transaction parseTransaction(String yamlContent) throws Exception {
        logger.debug("Parsing YAML transaction: {}", yamlContent);

        Transaction transaction = yamlMapper.readValue(yamlContent, Transaction.class);

        logger.debug("Parsed transaction: {}", transaction);

        return transaction;
    }
}
