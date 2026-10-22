package com.gracefulshutdown.procr.util;

import com.gracefulshutdown.procr.domain.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * YamlSerializer serializes Response object to YAML
 */
@Component
public class YamlSerializer {
    private static final Logger logger = LoggerFactory.getLogger(YamlSerializer.class);
    private final ObjectMapper yamlMapper;

    public YamlSerializer() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public String serializeResponse(Response response) throws Exception {
        logger.debug("Serializing response to YAML: {}", response);

        String yamlContent = yamlMapper.writeValueAsString(response);

        logger.debug("Serialized YAML: {}", yamlContent);

        return yamlContent;
    }
}
