package com.gracefulshutdown.central.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.gracefulshutdown.central.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class YamlSerializer {
    private static final Logger logger = LoggerFactory.getLogger(YamlSerializer.class);
    private final ObjectMapper yamlMapper;

    public YamlSerializer() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public String serialize(Object object) throws Exception {
        return yamlMapper.writeValueAsString(object);
    }

    public <T> T deserialize(String yaml, Class<T> clazz) throws Exception {
        return yamlMapper.readValue(yaml, clazz);
    }

    public String serializeTransaction(Transaction txn) throws Exception {
        return serialize(txn);
    }

    public Transaction deserializeTransaction(String yaml) throws Exception {
        return deserialize(yaml, Transaction.class);
    }
}
