package com.gracefulshutdown.procr.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * YAML Configuration for Spring REST controllers
 * Registers YAML media type handler for request/response conversion
 */
@Configuration
public class YamlConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        converters.add(new YamlHttpMessageConverter(yamlObjectMapper));
    }

    /**
     * Custom HttpMessageConverter for YAML serialization/deserialization
     */
    public static class YamlHttpMessageConverter extends AbstractJackson2HttpMessageConverter {
        public YamlHttpMessageConverter(ObjectMapper objectMapper) {
            super(objectMapper, org.springframework.http.MediaType.TEXT_PLAIN,
                  org.springframework.http.MediaType.parseMediaType("application/yaml"),
                  org.springframework.http.MediaType.parseMediaType("application/x-yaml"));
        }
    }
}
